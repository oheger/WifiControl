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
package com.github.oheger.wificontrol.domain.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

import java.net.InetAddress

class ServiceDefinitionTest : StringSpec({
    "multicastInetAddress should return the correct address" {
        val address = "231.10.1.2"
        val expectedInetAddress = InetAddress.getByName("231.10.1.2")

        val serviceDefinition = ServiceDefinition(
            "someName",
            ServiceAddressMode.WIFI_DISCOVERY,
            address,
            5000,
            "code",
            ""
        )

        serviceDefinition.multicastInetAddress shouldBe expectedInetAddress
    }
})
