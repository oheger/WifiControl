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
package com.github.oheger.wificontrol.domain.usecase

import com.github.oheger.wificontrol.domain.model.WiFiState
import com.github.oheger.wificontrol.domain.repo.WiFiStateRepository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainInOrder

import io.mockk.every
import io.mockk.mockk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList

class GetWiFiStateUseCaseTest : StringSpec({
    "A flow with the current Wi-Fi state should be returned" {
        val repository = mockk<WiFiStateRepository> {
            every { getWiFiState() } returns flowOf(WiFiState.WI_FI_UNAVAILABLE, WiFiState.WI_FI_AVAILABLE)
        }

        val useCase = GetWiFiStateUseCase(useCaseConfig, repository)
        val stateFlow = useCase.execute(GetWiFiStateUseCase.Input)

        stateFlow.toList() shouldContainInOrder listOf(
            Result.success(GetWiFiStateUseCase.Output(WiFiState.WI_FI_UNAVAILABLE)),
            Result.success(GetWiFiStateUseCase.Output(WiFiState.WI_FI_AVAILABLE))
        )
    }
})

/** The default configuration for test use case instances. */
private val useCaseConfig = UseCaseConfig(Dispatchers.Unconfined)
