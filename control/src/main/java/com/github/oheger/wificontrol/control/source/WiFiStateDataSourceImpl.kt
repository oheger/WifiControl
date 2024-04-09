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

import com.github.oheger.wificontrol.control.source.WiFiStateDataSourceImpl.Companion.create
import com.github.oheger.wificontrol.repository.ds.WiFiStateDataSource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking

/**
 * An implementation of the [WiFiStateDataSource] interface that registers a network callback on the
 * [ConnectivityManager] to receive notifications about changed Wi-Fi connectivity. These notifications are then
 * propagated to clients via a shared flow.
 *
 * There should be a central singleton instance of this class that is created via the [create] factory function of
 * the companion object.
 */
class WiFiStateDataSourceImpl : ConnectivityManager.NetworkCallback(), WiFiStateDataSource {
    companion object {
        fun create(context: Context): WiFiStateDataSource =
            WiFiStateDataSourceImpl().also {
                val connMan = context.getSystemService(ConnectivityManager::class.java)
                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build()

                it.reportStateChange(false)
                connMan.registerNetworkCallback(request, it)
            }
    }

    /** The flow to communicate changes in the connectivity state. */
    private val connectivityFlow = MutableSharedFlow<Boolean>(replay = 1)

    override fun loadWiFiAvailability(): Flow<Boolean> = connectivityFlow

    override fun onAvailable(network: Network) {
        reportStateChange(true)
    }

    override fun onLost(network: Network) {
        reportStateChange(false)
    }

    /**
     * Update the flow with the connectivity state for the given [newState].
     */
    private fun reportStateChange(newState: Boolean) {
        runBlocking { connectivityFlow.emit(newState) }
    }
}
