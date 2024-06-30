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

import com.github.oheger.wificontrol.domain.model.ServiceData
import com.github.oheger.wificontrol.domain.model.UndefinedCurrentService
import com.github.oheger.wificontrol.domain.usecase.LoadServiceUseCase
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
 * A test class for [ServiceDetailsViewModel] that focuses on functionality not covered by UI tests.
 */
@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class ServiceDetailsViewModelTest : StringSpec({
    val testDispatcher = newSingleThreadContext("testDispatcher")

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
    }

    "Data about the current service should only be loaded once" {
        val serviceIndex = 23
        val expectedInput = LoadServiceUseCase.Input(serviceIndex)
        val loadResult = LoadServiceUseCase.Output(ServiceData(emptyList()), mockk())
        val loadUseCase = mockk<LoadServiceUseCase> {
            every { execute(expectedInput) } returns flowOf(Result.success(loadResult))
        }
        val storeCurrentServiceUseCase = mockk<StoreCurrentServiceUseCase> {
            every {
                execute(StoreCurrentServiceUseCase.Input(UndefinedCurrentService))
            } returns flowOf(Result.failure(IllegalArgumentException("Test exception: store current service.")))
        }
        val viewModel = ServiceDetailsViewModel(loadUseCase, mockk(), storeCurrentServiceUseCase)

        viewModel.loadService(serviceIndex)
        viewModel.uiStateFlow.first()
        viewModel.loadService(serviceIndex)

        verify(exactly = 1, timeout = 3000) {
            loadUseCase.execute(expectedInput)
            storeCurrentServiceUseCase.execute(StoreCurrentServiceUseCase.Input(UndefinedCurrentService))
        }
    }
})
