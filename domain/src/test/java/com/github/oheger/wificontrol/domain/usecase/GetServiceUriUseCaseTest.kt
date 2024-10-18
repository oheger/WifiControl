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
import com.github.oheger.wificontrol.domain.model.LookupInProgress
import com.github.oheger.wificontrol.domain.model.LookupService
import com.github.oheger.wificontrol.domain.model.LookupSucceeded
import com.github.oheger.wificontrol.domain.model.ServiceAddressMode
import com.github.oheger.wificontrol.domain.model.ServiceDefinition
import com.github.oheger.wificontrol.domain.repo.ServiceUriRepository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

import kotlin.time.Duration.Companion.seconds

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock

class GetServiceUriUseCaseTest : StringSpec({
    "The flow from the repository should be returned" {
        val state1 = LookupInProgress(Clock.System.now(), 17)
        val state2 = LookupSucceeded("http://service.example.com/$SERVICE_NAME")
        val repository = mockk<ServiceUriRepository> {
            every { lookupService(SERVICE_NAME, any()) } returns flowOf(state1, state2)
        }

        val useCase = GetServiceUriUseCase(useCaseConfig, repository)
        val input = GetServiceUriUseCase.Input(
            serviceName = SERVICE_NAME,
            lookupServiceProvider = { throw UnsupportedOperationException("Unexpected call.") }
        )
        val stateFlow = useCase.execute(input)

        stateFlow.toList() shouldContainInOrder listOf(
            Result.success(GetServiceUriUseCase.Output(state1)),
            Result.success(GetServiceUriUseCase.Output(state2))
        )
    }

    "The callback for querying the lookup configuration should work correctly" {
        val repository = mockk<ServiceUriRepository> {
            every { lookupService(SERVICE_NAME, any()) } returns flowOf(LookupInProgress(Clock.System.now(), 1))
        }
        val lookupConfig = LookupConfig(
            lookupTimeout = 30.seconds,
            sendRequestInterval = 10.seconds
        )
        val lookupService = LookupService(
            service = ServiceDefinition(
                "test",
                ServiceAddressMode.WIFI_DISCOVERY,
                "231.1.1.1",
                10000,
                "code",
                ""
            ),
            lookupConfig = lookupConfig
        )
        val lookupServiceProvider: suspend () -> LookupService = { lookupService }

        val useCase = GetServiceUriUseCase(useCaseConfig, repository)
        useCase.execute(GetServiceUriUseCase.Input(SERVICE_NAME, lookupServiceProvider)).first()

        val slotCallback = slot<suspend () -> LookupService>()
        verify {
            repository.lookupService(SERVICE_NAME, capture(slotCallback))
        }

        val callbackResult = slotCallback.captured()
        callbackResult shouldBe lookupService
    }
})

/** The default configuration for test use case instances. */
private val useCaseConfig = UseCaseConfig(Dispatchers.Unconfined)

/** The name of the test service. */
private const val SERVICE_NAME = "MyServiceToLookup"
