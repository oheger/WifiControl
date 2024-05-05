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

import com.github.oheger.wificontrol.domain.model.LookupConfig
import com.github.oheger.wificontrol.domain.model.LookupService
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

class LoadServiceByNameUseCaseTest : StringSpec({
    "A service should be loaded successfully" {
        val serviceName = "theDesiredService"
        val service = createService(serviceName, 1)
        val serviceData = createServiceData(createService("other", 0), service, createService("yetAnother", 2))
        val loadDataUseCase = mockk<LoadServiceDataUseCase> {
            every { process(LoadServiceDataUseCase.Input) } returns flowOf(LoadServiceDataUseCase.Output(serviceData))
        }

        val useCase = LoadServiceByNameUseCase(useCaseConfig, loadDataUseCase)
        val resultFlow = useCase.execute(LoadServiceByNameUseCase.Input(serviceName))

        resultFlow.first().shouldBeSuccess(LoadServiceByNameUseCase.Output(service))
    }

    "A non existing service should cause a failure result" {
        val invalidName = "nonExistingService"
        val serviceData = createServiceData(createService("s1", 1), createService("s2", 2))
        val loadDataUseCase = mockk<LoadServiceDataUseCase> {
            every { process(LoadServiceDataUseCase.Input) } returns flowOf(LoadServiceDataUseCase.Output(serviceData))
        }

        val useCase = LoadServiceByNameUseCase(useCaseConfig, loadDataUseCase)
        val resultFlow = useCase.execute(LoadServiceByNameUseCase.Input(invalidName))

        resultFlow.first().shouldBeFailure { exception ->
            exception should beInstanceOf<IllegalArgumentException>()
            exception.message shouldContain invalidName
        }
    }

    "An exception from the wrapped use case should be handled" {
        val loadException = IllegalArgumentException("Loading failed.")
        val loadDataUseCase = mockk<LoadServiceDataUseCase> {
            every { process(LoadServiceDataUseCase.Input) } returns flow { throw loadException }
        }

        val useCase = LoadServiceByNameUseCase(useCaseConfig, loadDataUseCase)
        val resultFlow = useCase.execute(LoadServiceByNameUseCase.Input("foo"))

        resultFlow.first().shouldBeFailure { exception ->
            exception should beInstanceOf(loadException::class)
            exception.message shouldBe loadException.message
        }
    }
})

/** The default configuration for test use case instances. */
private val useCaseConfig = UseCaseConfig(Dispatchers.Unconfined)

/**
 * Create a [LookupService] for testing with the given [name] whose properties are derived from the given [index].
 */
private fun createService(name: String, index: Int): LookupService =
    LookupService(
        service = ServiceDefinition(
            name = name,
            multicastAddress = "231.10.0.$index",
            port = 8000 + index,
            requestCode = "code$index"
        ),
        lookupConfig = LookupConfig(
            networkTimeout = LookupConfig.DEFAULT_TIMEOUT,
            retryDelay = LookupConfig.DEFAULT_RETRY_DELAY,
            sendRequestInterval = LookupConfig.DEFAULT_SEND_REQUEST_INTERVAL
        )
    )

/**
 * Create a [ServiceData] object that contains the given [services].
 */
private fun createServiceData(vararg services: LookupService): ServiceData =
    ServiceData(
        services = services.map { lookupService ->
            PersistentService(
                serviceDefinition = lookupService.service,
                networkTimeout = null,
                retryDelay = null,
                sendRequestInterval = null
            )
        },
        0
    )
