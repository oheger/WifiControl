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
package com.github.oheger.wificontrol.controlui

import com.github.oheger.wificontrol.domain.model.DefinedCurrentService
import com.github.oheger.wificontrol.domain.model.WiFiState
import com.github.oheger.wificontrol.domain.usecase.GetWiFiStateUseCase
import com.github.oheger.wificontrol.domain.usecase.LoadServiceByNameUseCase
import com.github.oheger.wificontrol.domain.usecase.StoreCurrentServiceUseCase

import io.kotest.core.spec.style.StringSpec

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Test class for [ControlViewModel] that focuses on functionality not covered by UI tests.
 */
@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class ControlViewModelTest : StringSpec({
    val testDispatcher = newSingleThreadContext("testDispatcher")

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
    }

    "Initialization should be executed only once" {
        val serviceName = "someServiceToControl"
        val parameters = ControlViewModel.Parameters(serviceName)
        val wiFiStateUseCase = mockk<GetWiFiStateUseCase> {
            every {
                execute(GetWiFiStateUseCase.Input)
            } returns flowOf(Result.success(GetWiFiStateUseCase.Output(WiFiState.WI_FI_UNAVAILABLE)))
        }
        val loadServiceUseCase = mockk<LoadServiceByNameUseCase> {
            every {
                execute(LoadServiceByNameUseCase.Input(serviceName))
            } returns flowOf(Result.failure(IllegalStateException("Test exception: Load current service.")))
        }
        val storeCurrentServiceUseCase = mockk<StoreCurrentServiceUseCase> {
            every {
                execute(StoreCurrentServiceUseCase.Input(DefinedCurrentService(serviceName)))
            } returns flowOf(Result.failure(IllegalStateException("Test exception: Store current service")))
        }
        val viewModel = ControlViewModel(
            wiFiStateUseCase,
            mockk(),
            mockk(),
            loadServiceUseCase,
            storeCurrentServiceUseCase
        )

        viewModel.loadUiState(parameters)
        viewModel.uiStateFlow.first()
        viewModel.loadUiState(parameters)

        verify(exactly = 1, timeout = 3000) {
            wiFiStateUseCase.execute(GetWiFiStateUseCase.Input)
            loadServiceUseCase.execute(LoadServiceByNameUseCase.Input(serviceName))
            storeCurrentServiceUseCase.execute(StoreCurrentServiceUseCase.Input(DefinedCurrentService(serviceName)))
        }
    }
})
