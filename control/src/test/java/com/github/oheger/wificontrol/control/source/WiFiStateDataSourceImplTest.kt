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

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
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

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Test class for [WiFiStateDataSourceImpl].
 */
class WiFiStateDataSourceImplTest : StringSpec({
    beforeSpec {
        mockkConstructor(NetworkRequest.Builder::class)
    }

    afterSpec {
        unmockkAll()
    }

    "The initial state that no Wi-Fi is available should be reported" {
        val context = mockk<Context>()
        context.mockConnectivityManager()
        mockNetworkRequest()

        val source = WiFiStateDataSourceImpl.create(context)

        source.loadWiFiAvailability().first() shouldBe false
    }

    "Changes on the Wi-Fi connect status should be reported" {
        val network = mockk<Network>()
        val context = mockk<Context>()
        val connMan = context.mockConnectivityManager()

        val request = mockNetworkRequest()
        val source = WiFiStateDataSourceImpl.create(context)
        val flow = source.loadWiFiAvailability()

        val queue = LinkedBlockingQueue<Boolean>()
        fun expectState(expected: Boolean) {
            val actual = queue.poll(3, TimeUnit.SECONDS)
            actual shouldBe expected
        }

        withContext(Dispatchers.IO) {
            val collectJob = launch {
                flow.collect(queue::offer)
            }

            val callback = connMan.getNetworkCallback(request)

            expectState(false)
            callback.onAvailable(network)
            expectState(true)
            callback.onLost(network)
            expectState(false)
            callback.onAvailable(network)
            expectState(true)

            collectJob.cancel()
        }
    }

    "The last known Wi-Fi connect status should be reported" {
        val context = mockk<Context>()
        val connMan = context.mockConnectivityManager()

        val request = mockNetworkRequest()
        val source = WiFiStateDataSourceImpl.create(context)

        val callback = connMan.getNetworkCallback(request)
        callback.onAvailable(mockk())

        source.loadWiFiAvailability().first() shouldBe true
    }

    "Multiple clients should be served" {
        val network = mockk<Network>()
        val context = mockk<Context>()
        val connMan = context.mockConnectivityManager()

        val request = mockNetworkRequest()
        val source = WiFiStateDataSourceImpl.create(context)
        val callback = connMan.getNetworkCallback(request)
        source.loadWiFiAvailability().first() shouldBe false

        callback.onAvailable(network)
        source.loadWiFiAvailability().first() shouldBe true
        source.loadWiFiAvailability().first() shouldBe true

        callback.onLost(network)
        source.loadWiFiAvailability().first() shouldBe false
    }
})

/**
 * Create a mock [ConnectivityManager] and prepare it to expect a callback registration. Prepare this mock
 * [Context] to return this manager on request.
 */
private fun Context.mockConnectivityManager(): ConnectivityManager {
    val connectivityManager = mockk<ConnectivityManager> {
        every { registerNetworkCallback(any(), any<ConnectivityManager.NetworkCallback>()) } just runs
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

    verify {
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
