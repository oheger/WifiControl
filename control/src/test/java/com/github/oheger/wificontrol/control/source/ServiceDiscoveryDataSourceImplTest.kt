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

import com.github.oheger.wificontrol.domain.model.LookupConfig
import com.github.oheger.wificontrol.domain.model.LookupFailed
import com.github.oheger.wificontrol.domain.model.LookupInProgress
import com.github.oheger.wificontrol.domain.model.LookupService
import com.github.oheger.wificontrol.domain.model.LookupState
import com.github.oheger.wificontrol.domain.model.LookupSucceeded
import com.github.oheger.wificontrol.domain.model.ServiceAddressMode
import com.github.oheger.wificontrol.domain.model.ServiceDefinition

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.WordSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.MulticastSocket
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.times

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
class ServiceDiscoveryDataSourceImplTest : WordSpec() {
    /** The dispatcher to be used for the main context. */
    private lateinit var testDispatcher: ExecutorCoroutineDispatcher

    init {
        beforeTest {
            testDispatcher = newSingleThreadContext("testDispatcher")
            Dispatchers.setMain(testDispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
            testDispatcher.close()
        }

        "discoverService" should {
            "switch to the found state when the service replies" {
                withTestServer { serviceDefinition ->
                    val lookupService = LookupService(serviceDefinition, defaultLookupConfig)
                    val source = createSource()

                    val stateFlow = source.discoverService(SERVICE_NAME) { lookupService }

                    val resultState = stateFlow.dropWhile { it is LookupInProgress }.first()
                    resultState shouldBe LookupSucceeded(SERVER_URI)
                }
            }

            "switch to the not found state when the timeout is reached" {
                withTestServer { serviceDefinition ->
                    val lookupService = createServiceForFailedLookup(serviceDefinition, timeout = 10.milliseconds)
                    val source = createSource()

                    val stateFlow = source.discoverService(SERVICE_NAME) { lookupService }

                    val resultState = stateFlow.dropWhile { it is LookupInProgress }.first()
                    resultState shouldBe LookupFailed
                }
            }

            "send updates while the discovery operation is ongoing" {
                withTestServer { serviceDefinition ->
                    val lookupService = createServiceForFailedLookup(
                        serviceDefinition,
                        timeout = 250.milliseconds,
                        sendInterval = 10.milliseconds
                    )
                    val source = createSource()

                    val stateFlow = source.discoverService(SERVICE_NAME) { lookupService }
                        .terminateAtEndState()

                    val states = stateFlow.toList()
                    states.size shouldBeGreaterThan 3

                    val inProgressStates = states.dropLast(1).map { it as LookupInProgress }
                    val discoveryTime = inProgressStates[0].startTime
                    (Clock.System.now() - discoveryTime) shouldBeLessThan 10.seconds
                    (1..<inProgressStates.size).toList().forAll { idx ->
                        inProgressStates[idx].startTime shouldBe discoveryTime
                        inProgressStates[idx].attempts shouldBe inProgressStates[idx - 1].attempts + 1
                    }
                }
            }

            "take the send interval into account" {
                withTestServer { serviceDefinition ->
                    val lookupService = createServiceForFailedLookup(
                        serviceDefinition,
                        timeout = 100.milliseconds,
                        sendInterval = 250.milliseconds
                    )
                    val source = createSource()

                    val stateFlow = source.discoverService(SERVICE_NAME) { lookupService }
                        .terminateAtEndState()

                    val states = stateFlow.toList()
                    states shouldHaveSize 2
                }
            }

            "handle exceptions from the lookup service provider function" {
                withTestServer { _ ->
                    val source = createSource()

                    val stateFlow = source.discoverService(SERVICE_NAME) {
                        throw IllegalArgumentException("Test exception")
                    }

                    stateFlow.first() shouldBe LookupFailed
                }
            }

            "ignore an invalid URI response from the service" {
                withTestServer(response = "?!This is not a valid URL!?") { serviceDefinition ->
                    val lookupService = LookupService(
                        service = serviceDefinition,
                        lookupConfig = defaultLookupConfig.copy(lookupTimeout = 200.milliseconds)
                    )
                    val source = createSource()

                    val stateFlow = source.discoverService(SERVICE_NAME) { lookupService }
                        .terminateAtEndState()

                    val states = stateFlow.toList()
                    states.last() shouldBe LookupFailed
                }
            }

            "cache results" {
                val source = createSource()

                withTestServer { serviceDefinition ->
                    val lookupService = LookupService(serviceDefinition, defaultLookupConfig)

                    val stateFlow = source.discoverService(SERVICE_NAME) { lookupService }

                    val resultState = stateFlow.dropWhile { it is LookupInProgress }.first()
                    resultState shouldBe LookupSucceeded(SERVER_URI)
                }

                val cachedFlow = source.discoverService(SERVICE_NAME) {
                    throw UnsupportedOperationException("Unexpected invocation")
                }

                cachedFlow.first() shouldBe LookupSucceeded(SERVER_URI)
            }

            "always store a terminal state in the cached flow" {
                val source = createSource()

                withTestServer { serviceDefinition ->
                    val lookupConfig = defaultLookupConfig.copy(sendRequestInterval = 1.nanoseconds)
                    val lookupService = LookupService(serviceDefinition, lookupConfig)
                    source.discoverService(SERVICE_NAME) { lookupService }.terminateAtEndState().collect {}
                }

                val cachedFlow = source.discoverService(SERVICE_NAME) {
                    throw UnsupportedOperationException("Unexpected invocation")
                }

                cachedFlow.first() shouldBe LookupSucceeded(SERVER_URI)
            }
        }

        "refreshService" should {
            "remove a service from the cache" {
                withTestServer { serviceDefinition ->
                    val source = createSource()
                    val failedLookupService = createServiceForFailedLookup(
                        serviceDefinition,
                        timeout = 10.milliseconds
                    )
                    val failedFlow = source.discoverService(SERVICE_NAME) { failedLookupService }
                    failedFlow.dropWhile { it is LookupInProgress }.first()

                    source.refreshService(SERVICE_NAME)

                    val lookupService = LookupService(serviceDefinition, defaultLookupConfig)
                    val stateFlow = source.discoverService(SERVICE_NAME) { lookupService }

                    val resultState = stateFlow.dropWhile { it is LookupInProgress }.first()
                    resultState shouldBe LookupSucceeded(SERVER_URI)
                }
            }

            "cancel an ongoing service discovery job" {
                val requestCounter = AtomicInteger()
                val waitForIntervals = 5

                withTestServer(requestCounter = requestCounter) { serviceDefinition ->
                    val failedLookupService = createServiceForFailedLookup(
                        serviceDefinition,
                        timeout = 60.seconds,
                        sendInterval = 3.milliseconds
                    )
                    val source = createSource()
                    source.discoverService(SERVICE_NAME) { failedLookupService }
                    eventually(duration = 1.seconds) { requestCounter.get() shouldBeGreaterThan 0 }

                    source.refreshService(SERVICE_NAME)
                    val numberOfRequests = requestCounter.get()

                    delay(waitForIntervals * failedLookupService.lookupConfig.sendRequestInterval)
                    requestCounter.get() shouldBeLessThan numberOfRequests + waitForIntervals
                }
            }
        }
    }
}

/** A test service name. */
private const val SERVICE_NAME = "TestService"

/** The URI to be returned by the test server. */
private const val SERVER_URI = "http://192.168.0.1:8765"

/** The command that causes the test UDP server to exit itself. */
private const val EXIT_COMMAND = "exitService"

/** The default lookup configuration used by tests. */
private val defaultLookupConfig = LookupConfig(
    lookupTimeout = 1.seconds,
    sendRequestInterval = 10.milliseconds
)

/**
 * Find a free port that can be used to start the test server.
 */
private fun findUnusedPort(): Int = ServerSocket(0).use { it.localPort }

/**
 * Create a test instance of the data source.
 */
private fun createSource(): ServiceDiscoveryDataSourceImpl {
    val scope = CoroutineScope(Job())
    return ServiceDiscoveryDataSourceImpl(scope)
}

/**
 * Create a [LookupService] instance based on the given [serviceDefinition] that will cause a failed discovery
 * because it sends an incorrect request code. The [LookupConfig] can be adapted by specifying an alternative
 * [timeout] or [sendInterval]
 */
private fun createServiceForFailedLookup(
    serviceDefinition: ServiceDefinition,
    timeout: Duration = defaultLookupConfig.lookupTimeout,
    sendInterval: Duration = defaultLookupConfig.sendRequestInterval
): LookupService {
    val currentDefinition = serviceDefinition.copy(requestCode = "otherCode")
    val lookupConfig = defaultLookupConfig.copy(lookupTimeout = timeout, sendRequestInterval = sendInterval)
    return LookupService(currentDefinition, lookupConfig)
}

/**
 * Modify this infinite [Flow] to terminate when an end state is reached.
 */
private fun Flow<LookupState>.terminateAtEndState(): Flow<LookupState> =
    transformWhile { value ->
        emit(value)
        value is LookupInProgress
    }

/**
 * A class implementing a server that handles UDP requests. This is used by the test cases to test service
 * discovery.
 */
private class UdpServer(
    /** The answer to be sent on incoming requests. */
    private val answer: String,

    /** A counter for the number of requests received by this server. */
    private val requestCounter: AtomicInteger
) {
    /** The socket the server is listening on. */
    private val multicastSocket: MulticastSocket = MulticastSocket(findUnusedPort())

    /** The service definition defining where this service can be reached. */
    val serviceDefinition: ServiceDefinition = ServiceDefinition(
        name = "testServiceDefinition",
        addressMode = ServiceAddressMode.WIFI_DISCOVERY,
        multicastAddress = "231.10.1.2",
        port = multicastSocket.localPort,
        requestCode = "testServer",
        serviceUrl = ""
    )

    init {
        multicastSocket.joinGroup(serviceDefinition.multicastInetAddress)
    }

    /**
     * Start this server and handle incoming requests until the exit command it received.
     */
    fun start() {
        val buffer = ByteArray(256)
        val packet = DatagramPacket(buffer, buffer.size)
        var exit: Boolean

        do {
            multicastSocket.receive(packet)
            requestCounter.incrementAndGet()
            val request = String(packet.data, 0, packet.length)
            exit = when (request.substringBeforeLast(':')) {
                serviceDefinition.requestCode -> {
                    val port = request.substringAfterLast(':').toInt()
                    val packetAnswer = DatagramPacket(answer.toByteArray(), answer.length, packet.address, port)
                    multicastSocket.send(packetAnswer)
                    false
                }

                EXIT_COMMAND -> true
                else -> false
            }
        } while (!exit)

        multicastSocket.leaveGroup(serviceDefinition.multicastInetAddress)
    }

    /**
     * Stop this server by sending itself an exit command. This causes the request processing loop to exit.
     */
    fun shutdown() {
        val packetExit = DatagramPacket(
            EXIT_COMMAND.toByteArray(),
            EXIT_COMMAND.length,
            serviceDefinition.multicastInetAddress,
            serviceDefinition.port
        )
        DatagramSocket().use { socket ->
            socket.send(packetExit)
        }
    }
}

/**
 * Launch a test UDP server that answers requests with the given [response] and tracks the number of received
 * requests using the given [requestCounter]. Then execute [block], and finally wait for the termination of the test
 * server.
 */
private suspend fun withTestServer(
    response: String = SERVER_URI,
    requestCounter: AtomicInteger = AtomicInteger(),
    block: suspend (ServiceDefinition) -> Unit
) {
    withContext(Dispatchers.IO) {
        val (server, job) = startServer(response, requestCounter)
        try {
            block(server.serviceDefinition)
        } finally {
            server.shutdown()
            job.join()
        }
    }
}

/**
 * Create an instance of [UdpServer] and launch it in background. The server returns the given [response] when it
 * receives a valid request. The number of received requests is tracked using the given [requestCounter]. Return the
 * server object and the job that executes it.
 */
private fun CoroutineScope.startServer(response: String, requestCounter: AtomicInteger): Pair<UdpServer, Job> {
    val server = UdpServer(response, requestCounter)
    return server to launch(Dispatchers.IO) { server.start() }
}
