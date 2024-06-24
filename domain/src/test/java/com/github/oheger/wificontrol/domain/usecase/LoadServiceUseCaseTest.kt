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

import com.github.oheger.wificontrol.domain.model.PersistentService
import com.github.oheger.wificontrol.domain.model.ServiceData
import com.github.oheger.wificontrol.domain.model.ServiceDefinition

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.beInstanceOf

import io.mockk.every
import io.mockk.mockk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class LoadServiceUseCaseTest : StringSpec({
    "A service should be loaded successfully" {
        val service = createService(1)
        val serviceData = ServiceData(listOf(createService(0), service, createService(2)))
        val loadDataUseCase = mockk<LoadServiceDataUseCase> {
            every {
                process(LoadServiceDataUseCase.Input)
            } returns flowOf(LoadServiceDataUseCase.Output(serviceData))
        }

        val loadUseCase = LoadServiceUseCase(useCaseConfig, loadDataUseCase)
        val resultFlow = loadUseCase.execute(LoadServiceUseCase.Input(1))

        resultFlow.first().shouldBeSuccess(LoadServiceUseCase.Output(serviceData, service))
    }

    "The index for a new service should be handled correctly" {
        val serviceData = ServiceData(listOf(createService(1), createService(2)))
        val loadDataUseCase = mockk<LoadServiceDataUseCase> {
            every {
                process(LoadServiceDataUseCase.Input)
            } returns flowOf(LoadServiceDataUseCase.Output(serviceData))
        }

        val loadUseCase = LoadServiceUseCase(useCaseConfig, loadDataUseCase)
        val resultFlow = loadUseCase.execute(LoadServiceUseCase.Input(ServiceData.NEW_SERVICE_INDEX))

        resultFlow.first().shouldBeSuccess { output ->
            output.serviceData shouldBe serviceData
            output.service shouldBe PersistentService(
                serviceDefinition = ServiceDefinition("", "", 0, ""),
                lookupTimeout = null,
                sendRequestInterval = null
            )
        }
    }

    "A non existing service should cause a failure result" {
        val nonExistingIndex = 42
        val serviceData = ServiceData(listOf(createService(1), createService(2)))
        val loadDataUseCase = mockk<LoadServiceDataUseCase> {
            every {
                process(LoadServiceDataUseCase.Input)
            } returns flowOf(LoadServiceDataUseCase.Output(serviceData))
        }

        val loadUseCase = LoadServiceUseCase(useCaseConfig, loadDataUseCase)
        val resultFlow = loadUseCase.execute(LoadServiceUseCase.Input(42))

        resultFlow.first().shouldBeFailure { exception ->
            exception should beInstanceOf<ArrayIndexOutOfBoundsException>()
            exception.message shouldContain nonExistingIndex.toString()
        }
    }

    "An error from the load data use case should be handled" {
        val loadException = IllegalArgumentException("Loading failed.")
        val loadDataUseCase = mockk<LoadServiceDataUseCase> {
            every { process(LoadServiceDataUseCase.Input) } returns flow { throw loadException }
        }

        val loadUseCase = LoadServiceUseCase(useCaseConfig, loadDataUseCase)
        val resultFlow = loadUseCase.execute(LoadServiceUseCase.Input(0))

        resultFlow.first().shouldBeFailure { exception ->
            exception should beInstanceOf(loadException::class)
            exception.message shouldBe loadException.message
        }
    }
})

/** The default configuration for test use case instances. */
private val useCaseConfig = UseCaseConfig(Dispatchers.Unconfined)

/**
 * Create a [PersistentService] for testing whose properties are derived from the given [index].
 */
private fun createService(index: Int): PersistentService =
    PersistentService(
        serviceDefinition = ServiceDefinition(
            name = "service$index",
            multicastAddress = "231.10.0.$index",
            port = 8000 + index,
            requestCode = "code$index"
        ),
        lookupTimeout = null,
        sendRequestInterval = null
    )
