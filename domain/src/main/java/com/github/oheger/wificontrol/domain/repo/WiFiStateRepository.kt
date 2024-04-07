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
package com.github.oheger.wificontrol.domain.repo

import com.github.oheger.wificontrol.domain.model.WiFiState

import kotlinx.coroutines.flow.Flow

/**
 * Definition of a repository interface for keeping track on the current Wi-Fi connection state. This repository
 * provides a [Flow] of the connection state, so that clients can react on changes.
 */
interface WiFiStateRepository {
    /**
     * Return a [Flow] that can be used to monitor the current [WiFiState].
     */
    fun getWiFiState(): Flow<WiFiState>
}
