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

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PersistentServiceDataSerializerTest : StringSpec({
    "A correct default instance should be returned" {
        val defaultServiceData = PersistentServiceDataSerializer.defaultValue

        defaultServiceData.serviceDefinitionsCount shouldBe 0
    }

    "A round-trip with serialization and deserialization should work" {
        val service1 = PersistentServiceDefinition.newBuilder()
            .setName("testService1")
            .setMulticastAddress("231.10.17.11")
            .setPort(9999)
            .setRequestCode("testRequest1")
            .build()
        val service2 = PersistentServiceDefinition.newBuilder()
            .setName("testService2")
            .setAddressMode(ServiceAddressMode.MODE_WIFI_DISCOVERY)
            .setMulticastAddress("231.10.18.12")
            .setPort(11111)
            .setRequestCode("testRequest2")
            .setLookupTimeoutMs(10000)
            .setSendRequestIntervalMs(250)
            .build()
        val service3 = PersistentServiceDefinition.newBuilder()
            .setServiceUrl("testService3")
            .setServiceUrl("http://192.168.0.17/test.html")
            .setAddressMode(ServiceAddressMode.MODE_FIX_URL)
            .build()
        val data = PersistentServiceData.newBuilder()
            .addAllServiceDefinitions(listOf(service1, service2, service3))
            .build()

        val bos = ByteArrayOutputStream()
        PersistentServiceDataSerializer.writeTo(data, bos)

        val bin = ByteArrayInputStream(bos.toByteArray())
        val data2 = PersistentServiceDataSerializer.readFrom(bin)

        data2 shouldBe data
    }
})
