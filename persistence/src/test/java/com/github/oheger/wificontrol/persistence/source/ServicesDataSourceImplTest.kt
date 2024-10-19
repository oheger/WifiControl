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
package com.github.oheger.wificontrol.persistence.source

import androidx.datastore.core.DataStore

import com.github.oheger.wificontrol.domain.model.PersistentService
import com.github.oheger.wificontrol.domain.model.ServiceAddressMode as ModelServiceAddressMode
import com.github.oheger.wificontrol.domain.model.ServiceData
import com.github.oheger.wificontrol.domain.model.ServiceDefinition

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

class ServicesDataSourceImplTest : WordSpec({
    "loadServiceData" should {
        "return a flow with the current app state" {
            val dataStore = mockk<DataStore<PersistentServiceData>> {
                every { data } returns flowOf(persistentServiceData)
            }

            val source = ServicesDataSourceImpl(dataStore)
            val dataFromSource = source.loadServiceData().first()

            dataFromSource shouldBe modelServiceData
        }
    }

    "saveServiceData" should {
        "update the data store with the new instance" {
            val dataStore = mockk<DataStore<PersistentServiceData>> {
                coEvery { updateData(any()) } returns mockk()
            }

            val source = ServicesDataSourceImpl(dataStore)
            source.saveServiceData(modelServiceData)

            val slotTransform = slot<suspend (PersistentServiceData) -> PersistentServiceData>()
            coVerify {
                dataStore.updateData(capture(slotTransform))
            }

            // In persistentServiceData, the address mode is not initialized for all services.
            val expectedServiceData = persistentServiceData.toBuilder().setServiceDefinitions(
                0,
                persistentService1.toBuilder()
                    .setAddressMode(ServiceAddressMode.MODE_WIFI_DISCOVERY)
            ).build()
            slotTransform.captured(mockk()) shouldBe expectedServiceData
        }
    }
})

/** A persistent test service. */
private val persistentService1 = PersistentServiceDefinition.newBuilder()
    .setName("testService1")
    .setMulticastAddress("231.10.17.11")
    .setPort(9999)
    .setRequestCode("testRequest1")
    .build()

/** Another persistent test service. */
private val persistentService2 = PersistentServiceDefinition.newBuilder()
    .setName("testService2")
    .setAddressMode(ServiceAddressMode.MODE_WIFI_DISCOVERY)
    .setMulticastAddress("231.10.18.12")
    .setPort(11111)
    .setRequestCode("testRequest2")
    .setLookupTimeoutMs(10000)
    .setSendRequestIntervalMs(250)
    .build()

/** One more persistent service that uses a fix URL. */
private val persistentService3 = PersistentServiceDefinition.newBuilder()
    .setName("testService3")
    .setAddressMode(ServiceAddressMode.MODE_FIX_URL)
    .setServiceUrl("https://192.168.0.1/url-service/index.html")
    .build()

/** A persistent service data instance used by tests. */
private val persistentServiceData = PersistentServiceData.newBuilder()
    .addAllServiceDefinitions(listOf(persistentService1, persistentService2, persistentService3))
    .build()

/** A service from the domain layer corresponding to the first persistent test service. */
private val modelService1 = PersistentService(
    serviceDefinition = ServiceDefinition(
        name = "testService1",
        addressMode = ModelServiceAddressMode.WIFI_DISCOVERY,
        multicastAddress = "231.10.17.11",
        port = 9999,
        requestCode = "testRequest1",
        serviceUrl = ""
    ),
    lookupTimeout = null,
    sendRequestInterval = null
)

/** A service from the domain layer corresponding to the second persistent test service. */
private val modelService2 = PersistentService(
    serviceDefinition = ServiceDefinition(
        name = "testService2",
        addressMode = ModelServiceAddressMode.WIFI_DISCOVERY,
        multicastAddress = "231.10.18.12",
        port = 11111,
        requestCode = "testRequest2",
        serviceUrl = ""
    ),
    lookupTimeout = 10.seconds,
    sendRequestInterval = 250.milliseconds
)

/** A service from the domain layer corresponding to the third persistent test service. */
private val modelService3 = PersistentService(
    serviceDefinition = ServiceDefinition(
        name = "testService3",
        addressMode = ModelServiceAddressMode.FIX_URL,
        multicastAddress = "",
        port = 0,
        requestCode = "",
        serviceUrl = "https://192.168.0.1/url-service/index.html"
    ),
    lookupTimeout = null,
    sendRequestInterval = null
)

/** A service data instance from the domain layer corresponding to the persistent service data. */
private val modelServiceData = ServiceData(
    services = listOf(modelService1, modelService2, modelService3)
)
