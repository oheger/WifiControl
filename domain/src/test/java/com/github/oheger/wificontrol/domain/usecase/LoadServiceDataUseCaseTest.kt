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

import com.github.oheger.wificontrol.domain.model.ServiceData
import com.github.oheger.wificontrol.domain.repo.ServiceDataRepository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

import io.mockk.every
import io.mockk.mockk

import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class LoadServiceDataUseCaseTest : StringSpec({
    "The current ServiceData instance should be loaded successfully" {
        val serviceData = mockk<ServiceData>()
        val repository = mockk<ServiceDataRepository> {
            every { getServiceData() } returns flowOf(serviceData)
        }

        val useCase = LoadServiceDataUseCase(useCaseConfig, repository)
        val result = useCase.execute().first()

        result.shouldBeSuccess(serviceData)
    }

    "Error thrown by the repository should be handled" {
        val exception = IllegalStateException("Test exception from repository.")
        val repository = mockk<ServiceDataRepository> {
            every { getServiceData() } returns flow { throw exception }
        }

        val useCase = LoadServiceDataUseCase(useCaseConfig, repository)
        val result = useCase.execute().first()

        result.shouldBeFailure { actualException ->
            actualException.shouldBeInstanceOf<IllegalStateException>()
            actualException.message shouldBe exception.message
        }
    }

    "The dispatcher from the configuration should be used" {
        val scheduledTaskCount = AtomicInteger()
        val executor = Executor { runnable ->
            scheduledTaskCount.incrementAndGet()
            runnable.run()
        }
        val config = UseCaseConfig(executor.asCoroutineDispatcher())
        val repository = mockk<ServiceDataRepository> {
            every { getServiceData() } returns flowOf(mockk())
        }

        val useCase = LoadServiceDataUseCase(config, repository)
        useCase.execute().first()

        scheduledTaskCount.get() shouldBe 1
    }
})

/** The default configuration for test use case instances. */
private val useCaseConfig = UseCaseConfig(Dispatchers.Unconfined)
