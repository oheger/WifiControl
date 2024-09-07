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
package com.github.oheger.wificontrol.svcui

import com.github.oheger.wificontrol.domain.model.PersistentService
import com.github.oheger.wificontrol.domain.model.ServiceDefinition

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe

class ServiceEditModelTest : WordSpec({
    "service name" should {
        "be correctly initialized" {
            val model = ServiceEditModel(service)

            model.serviceName shouldBe serviceDefinition.name
            model.serviceNameValid shouldBe true
        }

        "be correctly validated" {
            val model = ServiceEditModel(service)

            model.serviceName = " "
            model.serviceNameValid shouldBe false
        }

        "not be marked as invalid initially" {
            val invalidNameDefinition = serviceDefinition.copy(name = "")
            val invalidService = service.copy(serviceDefinition = invalidNameDefinition)

            val model = ServiceEditModel(invalidService)

            model.serviceNameValid shouldBe true
        }

        "be handled correctly by validate" {
            val invalidNameDefinition = serviceDefinition.copy(name = "")
            val invalidService = service.copy(serviceDefinition = invalidNameDefinition)

            val model = ServiceEditModel(invalidService)
            model.validate() shouldBe false

            model.serviceNameValid shouldBe false
        }
    }

    "validateRequiredString" should {
        "return false for a string with trailing whitespace" {
            ServiceEditModel.validateRequiredString("TestService ") shouldBe false
        }

        "return false for a string with leading whitespace" {
            ServiceEditModel.validateRequiredString(" TestService") shouldBe false
        }
    }

    "multicast address" should {
        "be correctly initialized" {
            val model = ServiceEditModel(service)

            model.multicastAddress shouldBe serviceDefinition.multicastAddress
            model.multicastAddressValid shouldBe true
        }

        "be correctly validated" {
            val model = ServiceEditModel(service)

            model.multicastAddress = "x"
            model.multicastAddressValid shouldBe false
        }

        "not be marked as invalid initially" {
            val invalidAddressDefinition = serviceDefinition.copy(multicastAddress = "")
            val invalidService = service.copy(serviceDefinition = invalidAddressDefinition)

            val model = ServiceEditModel(invalidService)

            model.multicastAddressValid shouldBe true
        }

        "be handled correctly by validate" {
            val invalidAddressDefinition = serviceDefinition.copy(multicastAddress = "")
            val invalidService = service.copy(serviceDefinition = invalidAddressDefinition)

            val model = ServiceEditModel(invalidService)
            model.validate() shouldBe false

            model.multicastAddressValid shouldBe false
        }
    }

    "validateMulticastAddress" should {
        "return true for a valid IP address" {
            ServiceEditModel.validateMulticastAddress("231.10.0.7") shouldBe true
        }

        "return false if a component is not a number" {
            ServiceEditModel.validateMulticastAddress("1.2.3.a") shouldBe false
        }

        "return false if a component is out of range" {
            ServiceEditModel.validateMulticastAddress("1.2.256.128") shouldBe false
        }

        "return false if a component is missing" {
            ServiceEditModel.validateMulticastAddress("231.0.1") shouldBe false
        }

        "return false if there is a superfluous component" {
            ServiceEditModel.validateMulticastAddress("231.0.1.2.3") shouldBe false
        }
    }

    "port" should {
        "be correctly initialized" {
            val model = ServiceEditModel(service)

            model.port shouldBe serviceDefinition.port.toString()
            model.portValid shouldBe true
        }

        "be correctly validated" {
            val model = ServiceEditModel(service)

            model.port = "x"
            model.portValid shouldBe false
        }

        "not be marked as invalid initially" {
            val invalidPortDefinition = serviceDefinition.copy(port = -1)
            val invalidService = service.copy(serviceDefinition = invalidPortDefinition)

            val model = ServiceEditModel(invalidService)

            model.portValid shouldBe true
        }

        "be handled correctly by validate" {
            val invalidPortDefinition = serviceDefinition.copy(port = -1)
            val invalidService = service.copy(serviceDefinition = invalidPortDefinition)

            val model = ServiceEditModel(invalidService)
            model.validate() shouldBe false

            model.portValid shouldBe false
        }
    }

    "validatePort" should {
        "return false for a negative value" {
            ServiceEditModel.validatePort("-1") shouldBe false
        }

        "return false for a value too big" {
            ServiceEditModel.validatePort("65536") shouldBe false
        }

        "return false for a non-numeric value" {
            ServiceEditModel.validatePort("notAPortNumber") shouldBe false
        }
    }

    "code" should {
        "be correctly initialized" {
            val model = ServiceEditModel(service)

            model.code shouldBe serviceDefinition.requestCode
            model.codeValid shouldBe true
        }

        "be correctly validated" {
            val model = ServiceEditModel(service)

            model.code = ""
            model.codeValid shouldBe false
        }

        "not be marked as invalid initially" {
            val invalidCodeDefinition = serviceDefinition.copy(requestCode = "")
            val invalidService = service.copy(serviceDefinition = invalidCodeDefinition)

            val model = ServiceEditModel(invalidService)

            model.codeValid shouldBe true
        }

        "be handled correctly by validate" {
            val invalidCodeDefinition = serviceDefinition.copy(requestCode = "")
            val invalidService = service.copy(serviceDefinition = invalidCodeDefinition)

            val model = ServiceEditModel(invalidService)
            model.validate() shouldBe false

            model.codeValid shouldBe false
        }
    }

    "editedService" should {
        "return a correct service instance" {
            val changedDefinition = ServiceDefinition(
                name = "ChangedTestService",
                multicastAddress = "231.1.1.9",
                port = 8765,
                requestCode = "StillAnybodyOutThere?"
            )

            val model = ServiceEditModel(service)
            model.serviceName = changedDefinition.name
            model.multicastAddress = changedDefinition.multicastAddress
            model.port = changedDefinition.port.toString()
            model.code = changedDefinition.requestCode
            val changedService = model.editedService()

            changedService shouldBe PersistentService(
                serviceDefinition = changedDefinition,
                lookupTimeout = null,
                sendRequestInterval = null
            )
        }
    }
})

/** A service definition for a test service. */
private val serviceDefinition = ServiceDefinition(
    name = "TestService",
    multicastAddress = "231.0.0.8",
    port = 9876,
    requestCode = "AnybodyOutThere?"
)

/** A test service that should be edited. */
private val service = PersistentService(
    serviceDefinition = serviceDefinition,
    lookupTimeout = null,
    sendRequestInterval = null
)