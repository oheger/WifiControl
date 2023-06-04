/*
 * Copyright 2023 Oliver Heger.
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

import android.app.Activity
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A data class storing the configuration for [ServerFinder].
 */
data class ServerFinderConfig(
    /** The multicast address to which UDP requests need to be sent. */
    val multicastAddress: String,

    /** The port to send UDP multicast requests to. */
    val port: Int,

    /**
     * A code that becomes the payload of UDP requests. It is evaluated by the server. Only if the code matches, the
     * server sends a response.
     */
    val requestCode: String,

    /**
     * A timeout after which a test is considered a failure. It applies to multiple checks that wait for some kind of
     * response.
     */
    val networkTimeout: Duration,

    /**
     * A delay after which another attempt is made to contact the server after a failure.
     */
    val retryDelay: Duration
)

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
    /** The configuration for this instance. */
    val config: ServerFinderConfig,

    /** The current [ServerLookupState]. */
    val state: ServerLookupState = NetworkStatusUnknown
) {
    /**
     * Execute the next step to locate the server in the network and return an updated [ServerFinder] instance.
     * Interact with the given [activity] if necessary. From the [state] of the resulting instance, the caller can
     * figure out whether the server could be successfully be located or if further steps are necessary.
     */
    suspend fun findServerStep(activity: Activity): ServerFinder =
        when (state) {
            is ServerFound -> this
            is WiFiUnavailable -> withState(findWiFi(activity, 1.days))
            else -> withState(findWiFi(activity, config.networkTimeout))
        }

    /**
     * Execute the step to find the Wi-Fi network. This function is called for the states [NetworkStatusUnknown] and
     * [WiFiUnavailable]. The difference is that in the latter state the function waits longer for the network to
     * become available; the [timeout] is expected as parameter. The [ConnectivityManager] needed to check the
     * network status is obtained from the given [activity].
     */
    private suspend fun findWiFi(activity: Activity, timeout: Duration): ServerLookupState =
        withContext(Dispatchers.Main) {
            val channel = Channel<ServerLookupState>()
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    runBlocking { channel.send(SearchingInWiFi) }
                }
            }

            val connManager = activity.getSystemService(ConnectivityManager::class.java)
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
     * Return a [ServerFinder] instance with the same configuration, but the given [nextState].
     */
    private fun withState(nextState: ServerLookupState): ServerFinder = ServerFinder(config, nextState)
}
