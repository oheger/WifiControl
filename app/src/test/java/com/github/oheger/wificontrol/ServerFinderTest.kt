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

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
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
import java.util.concurrent.atomic.AtomicBoolean

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
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
            val finder = ServerFinder(finderConfig, state)

            val nextFinder = finder.findServerStep(mockk())

            nextFinder.config shouldBe finderConfig
            nextFinder.state shouldBe state
        }

        "The WiFiUnavailable state should be returned if no network is present" {
            val activity = mockk<Activity>()
            activity.mockConnectivityManager()
            mockNetworkRequest()

            val finder = ServerFinder(finderConfig)

            val nextFinder = finder.findServerStep(activity)

            nextFinder.config shouldBe finderConfig
            nextFinder.state shouldBe WiFiUnavailable
        }

        "The network callback should be removed if no network was found" {
            val activity = mockk<Activity>()
            val connManager = activity.mockConnectivityManager()
            val request = mockNetworkRequest()

            ServerFinder(finderConfig).findServerStep(activity)

            val callback = connManager.getNetworkCallback(request)
            verify(timeout = TIMEOUT_MS) {
                connManager.unregisterNetworkCallback(callback)
            }
        }

        "The SearchingInWiFi state should be returned if the network is available" {
            val config = finderConfig.copy(networkTimeout = TIMEOUT_MS.milliseconds)
            val activity = mockk<Activity>()
            val connManager = activity.mockConnectivityManager()
            val request = mockNetworkRequest()

            val deferredResult = findServerStepAsync(ServerFinder(config), activity)

            val callback = connManager.getNetworkCallback(request)
            callback.onAvailable(mockk())

            val result = withTimeout(TIMEOUT_MS) { deferredResult.await() }
            result.config shouldBe config
            result.state shouldBe SearchingInWiFi
        }

        "The network callback should be removed if the network is available" {
            val config = finderConfig.copy(networkTimeout = TIMEOUT_MS.milliseconds)
            val activity = mockk<Activity>()
            val connManager = activity.mockConnectivityManager()
            val request = mockNetworkRequest()

            val result = findServerStepAsync(ServerFinder(config), activity)

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

            val finder = ServerFinder(finderConfig, WiFiUnavailable)
            val deferredResult = findServerStepAsync(finder, activity)

            delay(finderConfig.networkTimeout * 2)

            deferredResult.isCompleted shouldBe false
            deferredResult.isCancelled shouldBe false
            deferredResult.cancel()
        }

        "The WiFiUnavailable state should switch to SearchingInWiFi if Wi-Fi becomes available" {
            val config = finderConfig.copy(networkTimeout = TIMEOUT_MS.milliseconds)
            val activity = mockk<Activity>()
            val connManager = activity.mockConnectivityManager()
            val request = mockNetworkRequest()

            val deferredResult = findServerStepAsync(ServerFinder(config, WiFiUnavailable), activity)

            val callback = connManager.getNetworkCallback(request)
            callback.onAvailable(mockk())

            val result = withTimeout(TIMEOUT_MS) { deferredResult.await() }
            result.config shouldBe config
            result.state shouldBe SearchingInWiFi
        }

        "The SearchingInWiFi state should switch to ServerNotFound if not answer is received in the timeout" {
            val finder = ServerFinder(finderConfig, SearchingInWiFi)

            val nextFinder = finder.findServerStep(mockk())

            nextFinder.state shouldBe ServerNotFound
        }

        "The SearchingInWiFi state should switch to ServerFound if the server replies" {
            val serverUri = "http://www.example.org/found"
            startServer(serverUri)

            eventually(duration = 3.seconds) {
                val finder = ServerFinder(finderConfig, SearchingInWiFi)

                val nextFinder = finder.findServerStep(mockk())

                nextFinder.state shouldBe ServerFound(serverUri)
            }
        }

        "The SearchingInWiFi state should switch to ServerNotFound if an invalid URL is received from the server" {
            startServer("?!This is not a valid URL!?")

            val finder = ServerFinder(finderConfig, SearchingInWiFi)

            val nextFinder = finder.findServerStep(mockk())

            nextFinder.state shouldBe ServerNotFound
        }

        "The ServerNotFound state should switch to SearchingInWiFi" {
            val config = finderConfig.copy(retryDelay = 10.milliseconds)
            val finder = ServerFinder(config, ServerNotFound)

            val nextFinder = finder.findServerStep(mockk())

            nextFinder.state shouldBe SearchingInWiFi
        }

        "The ServerNotFound state should not switch to a new state before the configured delay" {
            val config = finderConfig.copy(retryDelay = 60.seconds)
            val finder = ServerFinder(config, ServerNotFound)

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

/** The default configuration used by tests. */
private val finderConfig = ServerFinderConfig(
    multicastAddress = "231.10.1.2",
    port = findUnusedPort(),
    requestCode = "testServer",
    networkTimeout = 100.milliseconds,
    retryDelay = 1.seconds
)

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
 * Launch code in background that simulates a test server and answers a UDP request with the given [answer]. Make sure
 * to return only after the server is listening.
 */
private fun CoroutineScope.startServer(answer: String) {
    val flagActive = AtomicBoolean()

    launch(Dispatchers.IO) { handleUpdRequest(answer, flagActive) }

    runBlocking {
        eventually(3.seconds) {
            flagActive.get() shouldBe true
        }
    }
}

/**
 * Open a [DatagramSocket] and expect a request for lookup the test server. Set the given [flag] to *true* when the
 * server is ready. When the expected request is received, send a response with the given [answer].
 */
private fun handleUpdRequest(answer: String, flag: AtomicBoolean) {
    MulticastSocket(finderConfig.port).use { socket ->
        socket.joinGroup(finderConfig.multicastInetAddress)
        val buffer = ByteArray(256)
        val packet = DatagramPacket(buffer, buffer.size)

        flag.set(true)
        socket.receive(packet)

        val request = String(packet.data, 0, packet.length)
        if (request == finderConfig.requestCode) {
            val packetAnswer = DatagramPacket(answer.toByteArray(), answer.length, packet.address, packet.port)
            socket.send(packetAnswer)
        }

        socket.leaveGroup(finderConfig.multicastInetAddress)
    }
}
