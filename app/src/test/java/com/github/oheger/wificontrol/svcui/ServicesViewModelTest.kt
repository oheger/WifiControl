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
import com.github.oheger.wificontrol.domain.usecase.LoadServiceDataUseCase

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
        val viewModel = ServicesViewModel(loadUseCase, mockk(), mockk())

        viewModel.loadServices()
        viewModel.uiStateFlow.first()
        viewModel.loadServices()

        verify(exactly = 1, timeout = 3000) {
            loadUseCase.execute(LoadServiceDataUseCase.Input)
        }
    }
})
