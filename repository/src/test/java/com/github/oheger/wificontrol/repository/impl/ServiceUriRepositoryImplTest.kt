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
package com.github.oheger.wificontrol.repository.impl

import com.github.oheger.wificontrol.domain.model.LookupFailed
import com.github.oheger.wificontrol.domain.model.LookupInProgress
import com.github.oheger.wificontrol.domain.model.LookupService
import com.github.oheger.wificontrol.domain.model.LookupState
import com.github.oheger.wificontrol.domain.model.LookupSucceeded
import com.github.oheger.wificontrol.repository.ds.ServiceDiscoveryDataSource

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainExactly

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock

class ServiceUriRepositoryImplTest : WordSpec({
    "lookupService" should {
        suspend fun testLookupService(flowFromSource: Flow<LookupState>, expectedValues: List<LookupState>) {
            val lookupServiceProvider = mockk<suspend () -> LookupService>()
            val source = mockk<ServiceDiscoveryDataSource> {
                every { discoverService(SERVICE_NAME, lookupServiceProvider) } returns flowFromSource
            }

            val repository = ServiceUriRepositoryImpl(source)
            val stateFlow = repository.lookupService(SERVICE_NAME, lookupServiceProvider)

            stateFlow.toList() shouldContainExactly expectedValues
        }

        "obtain a flow with the lookup state from the data source" {
            val stateValues = listOf(
                LookupInProgress(Clock.System.now(), 1),
                LookupSucceeded("http://service.example.com/test/service.html")
            )
            val sourceFlow = stateValues.asFlow()

            testLookupService(sourceFlow, stateValues)
        }

        "terminate the flow after receiving a success state" {
            val successState = LookupSucceeded("http://service.example.com/success/service.html")
            val sourceFlow = flowOf(successState, LookupInProgress(Clock.System.now(), 2))

            testLookupService(sourceFlow, listOf(successState))
        }

        "terminate the flow after receiving a failed state" {
            val sourceFlow = flowOf(LookupFailed, LookupInProgress(Clock.System.now(), 2))

            testLookupService(sourceFlow, listOf(LookupFailed))
        }
    }

    "clearService" should {
        "refresh the service in the data source" {
            val source = mockk<ServiceDiscoveryDataSource> {
                every { refreshService(SERVICE_NAME) } just runs
            }

            val repository = ServiceUriRepositoryImpl(source)
            repository.clearService(SERVICE_NAME)

            verify {
                source.refreshService(SERVICE_NAME)
            }
        }
    }
})

/** The name of the service used within test cases. */
private const val SERVICE_NAME = "ServiceToDiscover"
