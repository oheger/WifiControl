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

import android.app.Activity
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest

import com.github.oheger.wificontrol.domain.model.LookupConfig
import com.github.oheger.wificontrol.domain.model.LookupService
import com.github.oheger.wificontrol.domain.model.ServiceDefinition

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.MulticastSocket
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
class ServerFinderTest : StringSpec() {
    private lateinit var testDispatcher: ExecutorCoroutineDispatcher

    init {
        beforeSpec {
            mockkConstructor(NetworkRequest.Builder::class)
        }

        beforeTest {
            testDispatcher = newSingleThreadContext("testDispatcher")
            Dispatchers.setMain(testDispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
            testDispatcher.close()
        }

        afterSpec {
            unmockkAll()
        }

        "The final state should be returned directly" {
            val state = ServerFound(SERVER_URI)
            val lookupService = finderConfig()
            val finder = ServerFinder(lookupService, state)

            val nextFinder = finder.findServerStep(mockk())

            nextFinder.lookupService shouldBe lookupService
            nextFinder.state shouldBe state
        }

        "The WiFiUnavailable state should be returned if no network is present" {
            val activity = mockk<Activity>()
            activity.mockConnectivityManager()
            mockNetworkRequest()

            val lookupService = finderConfig()
            val finder = ServerFinder(lookupService)

            val nextFinder = finder.findServerStep(activity)

            nextFinder.lookupService shouldBe lookupService
            nextFinder.state shouldBe WiFiUnavailable
        }

        "The network callback should be removed if no network was found" {
            val activity = mockk<Activity>()
            val connManager = activity.mockConnectivityManager()
            val request = mockNetworkRequest()

            ServerFinder(finderConfig()).findServerStep(activity)

            val callback = connManager.getNetworkCallback(request)
            verify(timeout = TIMEOUT_MS) {
                connManager.unregisterNetworkCallback(callback)
            }
        }

        "The SearchingInWiFi state should be returned if the network is available" {
            val config = defaultLookupConfig.copy(networkTimeout = TIMEOUT_MS.milliseconds)
            val activity = mockk<Activity>()
            val connManager = activity.mockConnectivityManager()
            val request = mockNetworkRequest()

            val lookupService = finderConfig(lookupConfig = config)
            val deferredResult = findServerStepAsync(ServerFinder(lookupService), activity)

            val callback = connManager.getNetworkCallback(request)
            callback.onAvailable(mockk())

            val result = withTimeout(TIMEOUT_MS) { deferredResult.await() }
            result.lookupService shouldBe lookupService
            result.state shouldBe SearchingInWiFi
        }

        "The network callback should be removed if the network is available" {
            val config = defaultLookupConfig.copy(networkTimeout = TIMEOUT_MS.milliseconds)
            val activity = mockk<Activity>()
            val connManager = activity.mockConnectivityManager()
            val request = mockNetworkRequest()

            val result = findServerStepAsync(ServerFinder(finderConfig(lookupConfig = config)), activity)

            val callback = connManager.getNetworkCallback(request)
            callback.onAvailable(mockk())

            verify(timeout = TIMEOUT_MS) {
                connManager.unregisterNetworkCallback(callback)
            }
            result.await()
        }

        "The WiFiUnavailable state should ignore the network timeout" {
            val activity = mockk<Activity>()
            activity.mockConnectivityManager()
            mockNetworkRequest()

            val finder = ServerFinder(finderConfig(), WiFiUnavailable)
            val deferredResult = findServerStepAsync(finder, activity)

            delay(defaultLookupConfig.networkTimeout * 2)

            deferredResult.isCompleted shouldBe false
            deferredResult.isCancelled shouldBe false
            deferredResult.cancel()
        }

        "The WiFiUnavailable state should switch to SearchingInWiFi if Wi-Fi becomes available" {
            val lookupConfig = defaultLookupConfig.copy(networkTimeout = TIMEOUT_MS.milliseconds)
            val finderConfig = finderConfig(lookupConfig = lookupConfig)
            val activity = mockk<Activity>()
            val connManager = activity.mockConnectivityManager()
            val request = mockNetworkRequest()

            val deferredResult = findServerStepAsync(ServerFinder(finderConfig, WiFiUnavailable), activity)

            val callback = connManager.getNetworkCallback(request)
            callback.onAvailable(mockk())

            val result = withTimeout(TIMEOUT_MS) { deferredResult.await() }
            result.lookupService shouldBe finderConfig
            result.state shouldBe SearchingInWiFi
        }

        "The SearchingInWiFi state should switch to ServerNotFound if no answer is received in the timeout" {
            val finder = ServerFinder(finderConfig(), SearchingInWiFi)

            val nextFinder = finder.findServerStep(mockk())

            nextFinder.state shouldBe ServerNotFound
        }

        "The SearchingInWiFi state should switch to ServerFound if the server replies" {
            val serverUri = "http://www.example.org/found"
            withTestServer(answerRequestFunc(serverUri)) {
                eventually(duration = 3.seconds) {
                    val finder = ServerFinder(finderConfig(), SearchingInWiFi)

                    val nextFinder = finder.findServerStep(mockk())

                    nextFinder.state shouldBe ServerFound(serverUri)
                }
            }
        }

        "Multiple requests should be sent to the server during a check" {
            val config = defaultLookupConfig.copy(sendRequestInterval = 2.milliseconds)
            val scope = CoroutineScope(Dispatchers.IO)

            try {
                scope.withTestServer(answerIth(3)) {
                    val finder = ServerFinder(finderConfig(lookupConfig = config), SearchingInWiFi)

                    val nextFinder = finder.findServerStep(mockk())

                    nextFinder.state shouldBe ServerFound(SERVER_URI)
                }
            } finally {
                scope.cancel()
            }
        }

        "The send request interval should be taken into account" {
            val config =
                defaultLookupConfig.copy(networkTimeout = 50.milliseconds, sendRequestInterval = 20.milliseconds)
            val scope = CoroutineScope(Dispatchers.IO)

            try {
                scope.withTestServer(answerIth(4)) {
                    val activity = mockk<Activity>()
                    val finder = ServerFinder(finderConfig(lookupConfig = config), SearchingInWiFi)

                    val nextFinder = finder.findServerStep(activity)

                    // Terminate the test server.
                    val config2 = defaultLookupConfig.copy(sendRequestInterval = 1.milliseconds)
                    val finder2 = ServerFinder(finderConfig(lookupConfig = config2), SearchingInWiFi)
                    finder2.findServerStep(activity)

                    nextFinder.state shouldBe ServerNotFound
                }
            } finally {
                scope.cancel()
            }
        }

        "The SearchingInWiFi state should switch to ServerNotFound if an invalid URL is received from the server" {
            withTestServer(answerRequestFunc("?!This is not a valid URL!?")) {
                val finder = ServerFinder(finderConfig(), SearchingInWiFi)

                val nextFinder = finder.findServerStep(mockk())

                nextFinder.state shouldBe ServerNotFound
            }
        }

        "The ServerNotFound state should switch to SearchingInWiFi" {
            val config = defaultLookupConfig.copy(retryDelay = 10.milliseconds)
            val finder = ServerFinder(finderConfig(lookupConfig = config), ServerNotFound)

            val nextFinder = finder.findServerStep(mockk())

            nextFinder.state shouldBe SearchingInWiFi
        }

        "The ServerNotFound state should not switch to a new state before the configured delay" {
            val config = defaultLookupConfig.copy(retryDelay = 60.seconds)
            val finder = ServerFinder(finderConfig(lookupConfig = config), ServerNotFound)

            val nextFinder = findServerStepAsync(finder, mockk())

            delay(100.milliseconds)

            nextFinder.isCompleted shouldBe false
            nextFinder.cancel()
        }
    }
}

/** The URI to be returned by the test server. */
private const val SERVER_URI = "http://192.168.0.1:8765"

/** A timeout for verifying asynchronous operations. */
private const val TIMEOUT_MS = 3000L

/** The service definition used by tests. */
private val serviceDefinition = ServiceDefinition(
    name = "testServiceDefinition",
    multicastAddress = "231.10.1.2",
    port = findUnusedPort(),
    requestCode = "testServer"
)

/** The default lookup configuration used by tests. */
private val defaultLookupConfig = LookupConfig(
    networkTimeout = 100.milliseconds,
    retryDelay = 1.seconds,
    sendRequestInterval = 1.seconds
)

/**
 * Return a [LookupService] object to be used as configuration for the [ServerFinder] based on the given
 * [service] and [lookupConfig].
 */
private fun finderConfig(
    service: ServiceDefinition = serviceDefinition,
    lookupConfig: LookupConfig = defaultLookupConfig
): LookupService =
    LookupService(service, lookupConfig)

/**
 * Find a free port that can be used to start the test server.
 */
private fun findUnusedPort(): Int = ServerSocket(0).use { it.localPort }

/**
 * Create a mock [ConnectivityManager] and prepare it to expect a callback registration. Prepare this mock
 * [Activity] to return this manager on request.
 */
private fun Activity.mockConnectivityManager(): ConnectivityManager {
    val connectivityManager = mockk<ConnectivityManager> {
        every { registerNetworkCallback(any(), any<ConnectivityManager.NetworkCallback>()) } just runs
        every { unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) } just runs
    }

    every { getSystemService(ConnectivityManager::class.java) } returns connectivityManager

    return connectivityManager
}

/**
 * Return the callback that has been registered at this [ConnectivityManager] mock. Expect that the given
 * [network request][expectedRequest] was passed when the callback was registered.
 */
private fun ConnectivityManager.getNetworkCallback(
    expectedRequest: NetworkRequest
): ConnectivityManager.NetworkCallback {
    val slotCallback = slot<ConnectivityManager.NetworkCallback>()

    verify(timeout = TIMEOUT_MS) {
        registerNetworkCallback(expectedRequest, capture(slotCallback))
    }

    return slotCallback.captured
}

/**
 * Return a mock for a [NetworkRequest] and prepare the builder to construct it.
 */
private fun mockNetworkRequest(): NetworkRequest {
    val request = mockk<NetworkRequest>()
    val builder = mockk<NetworkRequest.Builder>()
    every {
        anyConstructed<NetworkRequest.Builder>().addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
    } returns builder
    every { builder.build() } returns request

    return request
}

/**
 * Invoke the [ServerFinder.findServerStep] function asynchronously on the given [finder] object passing in the
 * specified [activity]. This can be used to execute some steps the finder is waiting for in parallel.
 */
private fun CoroutineScope.findServerStepAsync(finder: ServerFinder, activity: Activity): Deferred<ServerFinder> =
    async(Dispatchers.Default) {
        finder.findServerStep(activity)
    }

/**
 * A data class that defines the return value of the UDP handler function. Based on this result, the mock UDP server
 * decides which data to be sent to the caller and whether to terminate the server.
 */
private data class HandlerResult(
    /** An optional response string. *Null* means that no response should be sent. */
    val response: String?,

    /** Flag whether the server should stop itself. */
    val terminate: Boolean
)

/**
 * Definition of a function to handle a request to the UDP server. The function expects the request string received
 * from the client and returns a [HandlerResult] object defining the response of the server.
 */
private typealias HandlerFunc = (String) -> HandlerResult

/**
 * Launch a test UDP server that answers requests using the given [handlerFunc]. Then execute [block], and finally
 * wait for the termination of the test server.
 */
private fun CoroutineScope.withTestServer(handlerFunc: HandlerFunc, block: suspend () -> Unit) {
    val stateCtr = AtomicInteger()

    startServer(stateCtr, handlerFunc)

    try {
        runBlocking { block() }
    } finally {
        stateCtr.waitFor(2)
    }
}

/**
 * Launch code in background that simulates a test server and answers a UDP request with the given [handlerFunc].
 * Make sure to return only after the server is listening.
 */
private fun CoroutineScope.startServer(stateCtr: AtomicInteger, handlerFunc: HandlerFunc) {
    launch(Dispatchers.IO) { handleUdpRequest(stateCtr, handlerFunc) }

    stateCtr.waitFor(1)
}

/**
 * Open a [DatagramSocket] and expect requests for looking ip the test server. Use the given [stateCtr] to report the
 * current server state: A value of 1 means that the server is ready; a value of 2 means that it is terminated.
 * Handle incoming requests using the given [handlerFunc] that also determines when to stop the server.
 */
private fun handleUdpRequest(stateCtr: AtomicInteger, handlerFunc: HandlerFunc) {
    MulticastSocket(serviceDefinition.port).use { socket ->
        socket.joinGroup(serviceDefinition.multicastInetAddress)
        val buffer = ByteArray(256)
        val packet = DatagramPacket(buffer, buffer.size)
        var exit: Boolean
        stateCtr.incrementAndGet()

        do {
            socket.receive(packet)
            val request = String(packet.data, 0, packet.length)
            val port = request.substringAfterLast(':').toInt()
            val result = handlerFunc(request.substringBeforeLast(':'))
            result.response?.let { answer ->
                val packetAnswer = DatagramPacket(answer.toByteArray(), answer.length, packet.address, port)
                socket.send(packetAnswer)
            }
            exit = result.terminate
        } while (!exit)

        socket.leaveGroup(serviceDefinition.multicastInetAddress)
        stateCtr.incrementAndGet()
    }
}

/**
 * Wait for a while until this [AtomicInteger] has the provided [value].
 */
private fun AtomicInteger.waitFor(value: Int) {
    runBlocking {
        eventually(3.seconds) {
            get() shouldBeGreaterThanOrEqual value
        }
    }
}

/**
 * Return a [HandlerFunc] that answer a correct request with the specified [answer].
 */
private fun answerRequestFunc(answer: String): HandlerFunc = { request ->
    HandlerResult(response = answer.takeIf { request == serviceDefinition.requestCode }, terminate = true)
}

/**
 * Return a [HandlerFunc] that skips a given number of requests before it sends a standard response.
 */
private fun answerIth(count: Int): HandlerFunc {
    val requestCount = AtomicInteger()
    val resultContinue = HandlerResult(null, terminate = false)

    return { _ ->
        if (requestCount.incrementAndGet() >= count) HandlerResult(SERVER_URI, terminate = true)
        else resultContinue
    }
}
