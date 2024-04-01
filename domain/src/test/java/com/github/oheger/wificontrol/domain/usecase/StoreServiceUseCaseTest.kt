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
import io.kotest.matchers.types.beInstanceOf

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

class StoreServiceUseCaseTest : StringSpec({
    "A modified service should be stored" {
        val serviceIndex = 1
        val modifiedService = mockk<PersistentService>()
        val originalName = "theOriginalServiceName"
        val originalService = mockk<PersistentService> {
            every { serviceDefinition } returns ServiceDefinition(
                name = originalName,
                multicastAddress = "",
                port = 0,
                requestCode = ""
            )
        }

        val serviceDataNew = mockk<ServiceData>()
        val serviceDataOrg = mockk<ServiceData> {
            every { updateService(originalName, modifiedService) } returns serviceDataNew
            every { services } returns listOf(mockk(), originalService, mockk())
        }

        val expectedInput = StoreServiceDataUseCase.Input(serviceDataNew)
        val storeDataUseCase = mockk<StoreServiceDataUseCase> {
            every {
                execute(expectedInput)
            } returns flowOf(Result.success(StoreServiceDataUseCase.Output))
        }

        val input = StoreServiceUseCase.Input(serviceDataOrg, modifiedService, serviceIndex)
        val useCase = StoreServiceUseCase(useCaseConfig, storeDataUseCase)
        val result = useCase.execute(input).first()

        result.shouldBeSuccess(StoreServiceUseCase.Output)
        verify {
            storeDataUseCase.execute(expectedInput)
        }
    }

    "A new service should be stored" {
        val newService = mockk<PersistentService>()
        val serviceDataNew = mockk<ServiceData>()
        val serviceDataOrg = mockk<ServiceData> {
            every { addService(newService) } returns serviceDataNew
        }

        val expectedInput = StoreServiceDataUseCase.Input(serviceDataNew)
        val storeDataUseCase = mockk<StoreServiceDataUseCase> {
            every {
                execute(expectedInput)
            } returns flowOf(Result.success(StoreServiceDataUseCase.Output))
        }

        val input = StoreServiceUseCase.Input(serviceDataOrg, newService, ServiceData.NEW_SERVICE_INDEX)
        val useCase = StoreServiceUseCase(useCaseConfig, storeDataUseCase)
        val result = useCase.execute(input).first()

        result.shouldBeSuccess(StoreServiceUseCase.Output)
        verify {
            storeDataUseCase.execute(expectedInput)
        }
    }

    "An exception thrown by the ServiceData is handled" {
        val serviceData = ServiceData(emptyList(), 0)

        val input = StoreServiceUseCase.Input(serviceData, mockk(), 1)
        val useCase = StoreServiceUseCase(useCaseConfig, mockk())
        val result = useCase.execute(input).first()

        result.shouldBeFailure()
    }

    "An exception thrown by the store data use case is handled" {
        val service = PersistentService(
            serviceDefinition = ServiceDefinition(
                name = "someService",
                multicastAddress = "1.2.3.4",
                port = 10000,
                requestCode = "code"
            ),
            networkTimeout = null,
            retryDelay = null,
            sendRequestInterval = null
        )
        val serviceData = ServiceData(listOf(service), 0)

        val expException = IllegalArgumentException("Could not store service data.")
        val storeDataUseCase = mockk<StoreServiceDataUseCase> {
            every { execute(any()) } throws expException
        }

        val input = StoreServiceUseCase.Input(serviceData, service, 0)
        val useCase = StoreServiceUseCase(useCaseConfig, storeDataUseCase)
        val result = useCase.execute(input).first()

        result.shouldBeFailure { exception ->
            exception should beInstanceOf(expException::class)
            exception.message shouldBe expException.message
        }
    }
})

/** The configuration passed to test use case instances. */
private val useCaseConfig = UseCaseConfig(Dispatchers.IO)
