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
import android.net.NetworkCapabilities
import android.net.NetworkRequest

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

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.newSingleThreadContext
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
    }
}

/** The URI to be returned by the test server. */
private const val SERVER_URI = "http://192.168.0.1:8765"

/** A timeout for verifying asynchronous operations. */
private const val TIMEOUT_MS = 3000L

/** The default configuration used by tests. */
private val finderConfig = ServerFinderConfig(
    multicastAddress = "231.10.1.2",
    port = 2213,
    requestCode = "testServer",
    networkTimeout = 100.milliseconds,
    retryDelay = 1.seconds
)

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
