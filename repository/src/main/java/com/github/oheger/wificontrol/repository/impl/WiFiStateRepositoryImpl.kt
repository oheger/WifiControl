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
package com.github.oheger.wificontrol.repository.impl

import com.github.oheger.wificontrol.domain.model.WiFiState
import com.github.oheger.wificontrol.domain.repo.WiFiStateRepository
import com.github.oheger.wificontrol.repository.ds.WiFiStateDataSource

import javax.inject.Inject

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * An implementation of the [WiFiStateRepository] interface that uses a [WiFiStateDataSource] to expose a [Flow] with
 * the current Wi-Fi connection state.
 */
class WiFiStateRepositoryImpl @Inject constructor(
    private val wiFiDataSource: WiFiStateDataSource
) : WiFiStateRepository {
    override fun getWiFiState(): Flow<WiFiState> =
        wiFiDataSource.loadWiFiAvailability().map { available ->
            if (available) WiFiState.WI_FI_AVAILABLE
            else WiFiState.WI_FI_UNAVAILABLE
        }
}
