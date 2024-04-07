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
package com.github.oheger.wificontrol.repository.ds

import kotlinx.coroutines.flow.Flow

/**
 * Definition of a data source interface for keeping track on the current Wi-Fi connection state. This data source is
 * used by the repository implementation that exposes the Wi-Fi connection state.
 */
interface WiFiStateDataSource {
    /**
     * Return a [Flow] that indicates whether the Wi-Fi is currently available or not.
     */
    fun loadWiFiAvailability(): Flow<Boolean>
}
