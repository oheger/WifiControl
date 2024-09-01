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
package com.github.oheger.wificontrol.svcui

import com.github.oheger.wificontrol.domain.model.DefinedCurrentService
import com.github.oheger.wificontrol.domain.model.ServiceData
import com.github.oheger.wificontrol.domain.model.UndefinedCurrentService
import com.github.oheger.wificontrol.domain.usecase.LoadCurrentServiceUseCase
import com.github.oheger.wificontrol.domain.usecase.LoadServiceDataUseCase
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
 * A test class for [ServicesViewModel] that focuses on functionality that is not covered by UI tests.
 */
@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class ServicesViewModelTest : StringSpec({
    val testDispatcher = newSingleThreadContext("testDispatcher")

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
    }

    "Data about services should only be loaded once" {
        val loadUseCase = mockk<LoadServiceDataUseCase> {
            every {
                execute(LoadServiceDataUseCase.Input)
            } returns flowOf(Result.success(LoadServiceDataUseCase.Output(ServiceData(emptyList()))))
        }
        val loadCurrentServiceUseCase = mockk<LoadCurrentServiceUseCase> {
            every { execute(LoadCurrentServiceUseCase.Input) } returns flowOf()
        }
        val storeCurrentServiceUseCase = mockk<StoreCurrentServiceUseCase> {
            every {
                execute(StoreCurrentServiceUseCase.Input(UndefinedCurrentService))
            } returns flowOf(Result.failure(IllegalArgumentException("Test exception: Storing current service.")))
        }
        val viewModel = ServicesViewModel(loadUseCase, mockk(), loadCurrentServiceUseCase, storeCurrentServiceUseCase)

        viewModel.loadUiState(ServicesViewModel.Parameters)
        viewModel.uiStateFlow.first()
        verify(timeout = 3000) {
            storeCurrentServiceUseCase.execute(StoreCurrentServiceUseCase.Input(UndefinedCurrentService))
        }

        viewModel.loadUiState(ServicesViewModel.Parameters)

        verify(exactly = 1, timeout = 3000) {
            loadUseCase.execute(LoadServiceDataUseCase.Input)
            loadCurrentServiceUseCase.execute(any())
            storeCurrentServiceUseCase.execute(any())
        }
    }

    "The current service should only be stored if no current service was loaded" {
        val loadUseCase = mockk<LoadServiceDataUseCase> {
            every {
                execute(LoadServiceDataUseCase.Input)
            } returns flowOf(Result.success(LoadServiceDataUseCase.Output(ServiceData(emptyList()))))
        }
        val loadCurrentServiceUseCase = mockk<LoadCurrentServiceUseCase> {
            every {
                execute(LoadCurrentServiceUseCase.Input)
            } returns flowOf(
                Result.success(LoadCurrentServiceUseCase.Output(DefinedCurrentService("someCurrentService")))
            )
        }
        val storeCurrentServiceUseCase = mockk<StoreCurrentServiceUseCase>()
        val viewModel = ServicesViewModel(loadUseCase, mockk(), loadCurrentServiceUseCase, storeCurrentServiceUseCase)

        viewModel.loadUiState(ServicesViewModel.Parameters)
        viewModel.uiStateFlow.first()

        verify(timeout = 3000) {
            loadCurrentServiceUseCase.execute(any())
        }
        verify(exactly = 0, timeout = 1000) {
            storeCurrentServiceUseCase.execute(any())
        }
    }
})
