/*
 * Copyright 2023-2024 Oliver Heger.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */
package com.github.oheger.wificontrol

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

import com.github.oheger.wificontrol.domain.model.LookupService

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.URI

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A class responsible for locating a specific HTTP server in the Wi-Fi network.
 *
 * The class checks several conditions and performs specific steps in order to locate the server in the network:
 * - Wi-Fi must be enabled.
 * - Then it sends a multicast UPD request to the configured multicast address and port using the specified
 *   requestCode as payload. This is done periodically until an answer is retrieved.
 * - The payload of the answer is interpreted as the URL of the server in the network.
 *
 * The class offers a [findServerStep] function a client can invoke repeatedly. It returns on each change of the
 * [ServerLookupState]. When the state changes to [ServerFound] the find process is complete.
 */
class ServerFinder(
    /** The service to be looked up by this instance. */
    val lookupService: LookupService,

    /** The current [ServerLookupState]. */
    val state: ServerLookupState = NetworkStatusUnknown
) {
    companion object {
        /** The tag to e used for logging. */
        private const val TAG = "ServerFinder"

        /** The size of the packet for receiving UDP data. */
        private const val DATAGRAM_PACKET_SIZE = 256

        /**
         * Return a [ServerFound] object for the URL represented by this String if it is valid. Otherwise, return
         * *null*.
         */
        private fun String.toServerFound(): ServerFound? =
            takeIf { runCatching { URI(this) }.isSuccess }?.let(::ServerFound)
    }

    /**
     * Execute the next step to locate the server in the network and return an updated [ServerFinder] instance.
     * Interact with the given [context] if necessary. From the [state] of the resulting instance, the caller can
     * figure out whether the server could be successfully be located or if further steps are necessary.
     */
    suspend fun findServerStep(context: Context): ServerFinder {
        Log.i(TAG, "Invoking in state '$state'.")

        return when (state) {
            is ServerFound -> this
            is NetworkStatusUnknown -> withState(findWiFi(context, lookupService.lookupConfig.networkTimeout))
            is WiFiUnavailable -> withState(findWiFi(context, 1.days))
            is SearchingInWiFi -> withState(searchInWiFi())
            is ServerNotFound -> withState(waitForNextUdpRequest())
        }
    }

    /**
     * Execute the step to find the Wi-Fi network. This function is called for the states [NetworkStatusUnknown] and
     * [WiFiUnavailable]. The difference is that in the latter state the function waits longer for the network to
     * become available; the [timeout] is expected as parameter. The [ConnectivityManager] needed to check the
     * network status is obtained from the given [context].
     */
    private suspend fun findWiFi(context: Context, timeout: Duration): ServerLookupState =
        withContext(Dispatchers.Main) {
            val channel = Channel<ServerLookupState>()
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    runBlocking { channel.send(SearchingInWiFi) }
                }
            }

            val connManager = context.getSystemService(ConnectivityManager::class.java)
            try {
                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build()
                launch { connManager.registerNetworkCallback(request, callback) }

                withTimeoutOrNull(timeout) {
                    channel.receive()
                } ?: WiFiUnavailable
            } finally {
                connManager.unregisterNetworkCallback(callback)
            }
        }

    /**
     * Try to discover the server in the network by sending a multicast UDP request using the configured parameters.
     * If a response is received with a valid URL, the server lookup was successful. If an error or a timeout
     * occurs, the lookup needs to be repeated after a while.
     */
    private suspend fun searchInWiFi(): ServerLookupState = withContext(Dispatchers.IO) {
        DatagramSocket().use { sendSocket ->
            DatagramSocket().use { receiveSocket ->
                val channel = Channel<String>()
                launch { udpCommunication(sendSocket, receiveSocket, channel) }

                withTimeoutOrNull(lookupService.lookupConfig.networkTimeout) {
                    channel.receive()
                }?.toServerFound() ?: ServerNotFound
            }
        }
    }

    /**
     * Try to communicate with the server via a UDP multicast request. Use the given [sendSocket] to send the request,
     * and the given [receiveSocket] to receive the response. Due to race conditions, it could happen that a
     * response from the server already arrives before the listener for responses is installed. To deal with this,
     * requests are sent periodically with a configurable delay until either a response is received or the timeout is
     * reached. If a response is received, propagate it via [channel].
     */
    private suspend fun udpCommunication(
        sendSocket: DatagramSocket,
        receiveSocket: DatagramSocket,
        channel: Channel<String>
    ) =
        withContext(Dispatchers.IO) {
            val query = "${lookupService.service.requestCode}:${receiveSocket.localPort}"
            val packetSend = DatagramPacket(
                query.toByteArray(),
                query.length,
                lookupService.service.multicastInetAddress,
                lookupService.service.port
            )
            val packetReceive = DatagramPacket(ByteArray(DATAGRAM_PACKET_SIZE), DATAGRAM_PACKET_SIZE)

            launch {
                runCatching {
                    while (true) {
                        Log.i(TAG, "Sending request to server.")
                        sendSocket.send(packetSend)
                        delay(lookupService.lookupConfig.sendRequestInterval)
                    }
                }
            }

            runCatching {
                receiveSocket.receive(packetReceive)

                val data = String(packetReceive.data, 0, packetReceive.length)
                Log.i(TAG, "Received response from server: '$data'.")

                channel.send(data)
            }
        }

    /**
     * Wait for the configured retry delay before switching again to the [SearchingInWiFi] state.
     */
    private suspend fun waitForNextUdpRequest(): ServerLookupState {
        delay(lookupService.lookupConfig.retryDelay)
        return SearchingInWiFi
    }

    /**
     * Return a [ServerFinder] instance with the same configuration, but the given [nextState].
     */
    private fun withState(nextState: ServerLookupState): ServerFinder =
        ServerFinder(lookupService, nextState).also {
            Log.i(TAG, "Switching to new state '$nextState'.")
        }
}
