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
import com.github.oheger.wificontrol.repository.ds.WiFiStateDataSource

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainInOrder

import io.mockk.every
import io.mockk.mockk

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList

class WiFiStateRepositoryImplTest : WordSpec({
    "getWiFiState" should {
        "return a flow with the Wi-Fi state derived from the data source" {
            val dataSource = mockk<WiFiStateDataSource> {
                every { loadWiFiAvailability() } returns flowOf(false, true)
            }

            val repository = WiFiStateRepositoryImpl(dataSource)
            val flow = repository.getWiFiState()

            flow.toList() shouldContainInOrder listOf(WiFiState.WI_FI_UNAVAILABLE, WiFiState.WI_FI_AVAILABLE)
        }
    }
})
