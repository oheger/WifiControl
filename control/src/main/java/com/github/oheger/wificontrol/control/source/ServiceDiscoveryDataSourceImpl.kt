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
package com.github.oheger.wificontrol.control.source

import android.util.Log

import com.github.oheger.wificontrol.domain.model.LookupConfig
import com.github.oheger.wificontrol.domain.model.LookupFailed
import com.github.oheger.wificontrol.domain.model.LookupInProgress
import com.github.oheger.wificontrol.domain.model.LookupService
import com.github.oheger.wificontrol.domain.model.LookupState
import com.github.oheger.wificontrol.domain.model.LookupSucceeded
import com.github.oheger.wificontrol.repository.ds.ServiceDiscoveryDataSource

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.URI

import javax.inject.Inject

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock

/**
 * An implementation of the [ServiceDiscoveryDataSource] interface.
 *
 * This implementation handles the discovery of services in the network by sending multicast requests according to a
 * [LookupConfig]. Once a service has been discovered, its URL is cached, so that it can be accessed directly the next
 * time it is requested. The caching is done by storing the [Flow] used to propagate updates of the [LookupState];
 * here a shared flow is used that records the last state.
 *
 * Service discovery operations should continue running, even if the user leaves the UI to control the service. So,
 * the service may then be directly available when the user returns to the UI. This is achieved by using a dedicated
 * [CoroutineScope]. Here typically a scope with the lifetime of the app should be passed.
 */
class ServiceDiscoveryDataSourceImpl @Inject constructor(
    /** The scope in which to launch new coroutines for service discovery. */
    private val scope: CoroutineScope
) : ServiceDiscoveryDataSource {
    companion object {
        /** The tag to be used for logging. */
        private const val TAG = "ServiceDiscoveryDataSource"

        /** The size of the packet for receiving UDP data. */
        private const val DATAGRAM_PACKET_SIZE = 256

        /**
         * Check whether the given [serviceResponse] is a valid URI. If not, it is ignored, and service discovery
         * continues.
         */
        private fun isValidUri(serviceResponse: String): Boolean =
            runCatching { URI(serviceResponse) }.isSuccess
    }

    /**
     * A cache storing the state flows for the services that have been queried via the [discoverService] function. This
     * allows direct access to the current state of the discovery operation (and potential updates) when a service is
     * queried later again. After the end of a discovery operation, the terminal state remains in the flow and is made
     * available via the replay buffer. Using a [MutableStateFlow] for caching already requested services allows for
     * thread-safe updates.
     */
    private val discoveryFlows = MutableStateFlow(emptyMap<String, Flow<LookupState>>())

    override fun discoverService(
        serviceName: String,
        lookupServiceProvider: suspend () -> LookupService
    ): Flow<LookupState> =
        discoveryFlows.updateAndGet { currentFlows ->
            if (serviceName !in currentFlows) {
                currentFlows + (serviceName to runDiscovery(serviceName, lookupServiceProvider))
            } else {
                currentFlows
            }
        }.getValue(serviceName)

    override fun refreshService(serviceName: String) {
        discoveryFlows.update { currentFlows -> currentFlows - serviceName }
    }

    /**
     * Start a new discovery operation for the service with the given [serviceName] making use of the provided
     * [lookupServiceProvider]. Return the [Flow] emitting state updates about the operation.
     */
    private fun runDiscovery(
        serviceName: String,
        lookupServiceProvider: suspend () -> LookupService
    ): MutableSharedFlow<LookupState> {
        val lookupFlow = MutableSharedFlow<LookupState>(replay = 1)

        scope.launch {
            runCatching {
                val lookupService = lookupServiceProvider()
                Log.i(TAG, "Starting service discovery for '$serviceName'.")

                // Use a temporary flow to receive notifications from UDP communication. These notifications are
                // propagated to the actual lookupFlow, until a success state is received. Further events (that may
                // be caused by ongoing UDP send operations) are then ignored.
                val udpFlow = MutableSharedFlow<LookupState>(replay = 1)

                val lookupResult = DatagramSocket().use { sendSocket ->
                    DatagramSocket().use { receiveSocket ->
                        launch {
                            udpFlow.transformWhile { state ->
                                emit(state)
                                lookupFlow.emit(state)
                                state is LookupInProgress
                            }.collect {}
                        }
                        val udpJob = launch { udpCommunication(lookupService, sendSocket, receiveSocket, udpFlow) }

                        withTimeoutOrNull(lookupService.lookupConfig.lookupTimeout) {
                            lookupFlow.first { it is LookupSucceeded }
                        }.also {
                            udpJob.cancel()
                        }
                    }
                }

                if (lookupResult != null) {
                    Log.i(TAG, "Service discovery was successful for '$serviceName'.")
                } else {
                    Log.i(TAG, "Service '$serviceName' could not be discovered within the timeout.")
                    lookupFlow.emit(LookupFailed)
                }
            }.onFailure {
                Log.e(TAG, "Service discovery failed for '$serviceName': $it")
                lookupFlow.emit(LookupFailed)
            }
        }

        return lookupFlow
    }

    /**
     * Try to communicate with the service described by the given [lookupService] via a UDP multicast request. Use the
     * given [sendSocket] to send requests, and the given [receiveSocket] to receive the response. Requests are sent
     * periodically until a response is received. Via the given [stateFlow] updates are sent about the attempts to
     * reach the service. Here also the final success state is broadcast when the service could be discovered.
     */
    private suspend fun udpCommunication(
        lookupService: LookupService,
        sendSocket: DatagramSocket,
        receiveSocket: DatagramSocket,
        stateFlow: MutableSharedFlow<LookupState>
    ) {
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
                val startTime = Clock.System.now()
                var attemptCount = 0

                while (true) {
                    Log.i(TAG, "Sending request to service '${lookupService.service.name}'.")
                    stateFlow.emit(LookupInProgress(startTime, attemptCount))
                    runCatching {
                        sendSocket.send(packetSend)
                    }.onFailure {
                        Log.e(TAG, "Exception when sending request: $it.")
                    }
                    delay(lookupService.lookupConfig.sendRequestInterval)
                    attemptCount += 1
                }
            }

            runCatching {
                receiveSocket.receive(packetReceive)

                val data = String(packetReceive.data, 0, packetReceive.length)
                Log.i(TAG, "Received response from server: '$data'.")

                data.takeIf(::isValidUri)?.let {
                    stateFlow.emit(LookupSucceeded(it))
                }
            }
        }
    }
}
