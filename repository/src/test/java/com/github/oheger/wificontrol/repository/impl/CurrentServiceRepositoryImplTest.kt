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

import com.github.oheger.wificontrol.domain.model.DefinedCurrentService
import com.github.oheger.wificontrol.domain.model.UndefinedCurrentService
import com.github.oheger.wificontrol.repository.ds.CurrentServiceDataSource

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

class CurrentServiceRepositoryImplTest : WordSpec({
    "getCurrentService" should {
        "return a flow with a defined service if a service name is available" {
            val serviceName = "thePersistedCurrentService"
            val serviceSource = mockk<CurrentServiceDataSource> {
                every { loadCurrentServiceOnStartup() } returns flowOf(serviceName)
            }

            val repository = CurrentServiceRepositoryImpl(serviceSource)
            val currentService = repository.getCurrentService().first()

            currentService shouldBe DefinedCurrentService(serviceName)
        }

        "return a flow with an undefined service if a null service name is obtained from the data source" {
            val serviceSource = mockk<CurrentServiceDataSource> {
                every { loadCurrentServiceOnStartup() } returns flowOf(null)
            }

            val repository = CurrentServiceRepositoryImpl(serviceSource)
            val currentService = repository.getCurrentService().first()

            currentService shouldBe UndefinedCurrentService
        }
    }

    "setCurrentService" should {
        "persist a defined service" {
            val currentService = DefinedCurrentService("serviceToPersist")
            val persistedServiceName = "${currentService.serviceName}-andPersisted"
            val serviceSource = mockk<CurrentServiceDataSource> {
                coEvery { saveCurrentService(currentService.serviceName) } just runs
                every { loadCurrentServiceOnStartup() } returns flowOf(persistedServiceName)
            }

            val repository = CurrentServiceRepositoryImpl(serviceSource)
            val result = repository.setCurrentService(currentService).first()

            result shouldBe DefinedCurrentService(persistedServiceName)
            coVerify {
                serviceSource.saveCurrentService(currentService.serviceName)
            }
        }

        "persist an undefined service" {
            val serviceSource = mockk<CurrentServiceDataSource> {
                coEvery { saveCurrentService(null) } just runs
                every { loadCurrentServiceOnStartup() } returns flowOf(null)
            }

            val repository = CurrentServiceRepositoryImpl(serviceSource)
            val result = repository.setCurrentService(UndefinedCurrentService).first()

            result shouldBe UndefinedCurrentService
            coVerify {
                serviceSource.saveCurrentService(null)
            }
        }
    }
})
