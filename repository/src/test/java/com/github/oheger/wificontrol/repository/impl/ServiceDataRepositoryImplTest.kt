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

import com.github.oheger.wificontrol.domain.model.ServiceData
import com.github.oheger.wificontrol.repository.ds.ServicesDataSource

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

class ServiceDataRepositoryImplTest : WordSpec({
    "getServiceData" should {
        "return a Flow with ServiceData objects from the data source" {
            val serviceData = ServiceData(
                services = listOf(mockk(), mockk()),
                currentIndex = 1
            )
            val dataSource = mockk<ServicesDataSource> {
                every { loadServiceData() } returns flowOf(serviceData)
            }

            val repository = ServiceDataRepositoryImpl(dataSource)
            val dataFromRepository = repository.getServiceData().first()

            dataFromRepository shouldBe serviceData
        }
    }

    "saveServiceData" should {
        "save the data using the data source and return a Flow with the recent data" {
            val savedData = mockk<ServiceData>()
            val recentData = mockk<ServiceData>()
            val dataSource = mockk<ServicesDataSource> {
                every { loadServiceData() } returns flowOf(recentData)

                coEvery { saveServiceData(savedData) } just runs
            }

            val repository = ServiceDataRepositoryImpl(dataSource)
            val dataFromRepository = repository.saveServiceData(savedData).first()

            dataFromRepository shouldBe recentData
            coVerify {
                dataSource.saveServiceData(savedData)
            }
        }
    }
})
