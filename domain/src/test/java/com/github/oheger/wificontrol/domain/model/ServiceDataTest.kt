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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.beTheSameInstanceAs

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ServiceDataTest : WordSpec({
    "get" should {
        "return a LookupService if all lookup properties are defined" {
            val expectedLookupConfig = LookupConfig(
                persistentService1.lookupTimeout!!,
                persistentService1.sendRequestInterval!!
            )
            val data = createServiceData()

            val lookupService = data[0]

            lookupService.service shouldBe service1
            lookupService.lookupConfig shouldBe expectedLookupConfig
        }

        "return a LookupService with default lookup properties" {
            val expectedLookupConfig = LookupConfig(
                LookupConfig.DEFAULT_LOOKUP_TIMEOUT,
                LookupConfig.DEFAULT_SEND_REQUEST_INTERVAL
            )
            val data = createServiceData()

            val lookupService = data[1]

            lookupService.service shouldBe service2
            lookupService.lookupConfig shouldBe expectedLookupConfig
        }

        "return an existing LookupService by name" {
            val expectedLookupConfig = LookupConfig(
                persistentService1.lookupTimeout!!,
                persistentService1.sendRequestInterval!!
            )
            val data = createServiceData()

            val lookupService = data[service1.name]

            lookupService.shouldNotBeNull()
            lookupService.service shouldBe service1
            lookupService.lookupConfig shouldBe expectedLookupConfig
        }

        "return null for if the service name cannot be resolved" {
            val data = createServiceData()

            val lookupService = data["aNonExistingService"]

            lookupService should beNull()
        }
    }

    "getService" should {
        "return an existing service" {
            val expectedLookupConfig = LookupConfig(
                persistentService1.lookupTimeout!!,
                persistentService1.sendRequestInterval!!
            )
            val data = createServiceData()

            val lookupService = data.getService(service1.name)

            lookupService.service shouldBe service1
            lookupService.lookupConfig shouldBe expectedLookupConfig
        }

        "throw an exception if the service does not exist" {
            val unknownServiceName = "missingService"
            val data = createServiceData()

            val exception = shouldThrow<IllegalArgumentException> {
                data.getService(unknownServiceName)
            }

            exception.message shouldContain unknownServiceName
        }
    }

    "getPersistentService" should {
        "return an existing service" {
            val data = createServiceData()

            val service = data.getPersistentService(service1.name)

            service shouldBe persistentService1
        }

        "throw an exception if the service does not exist" {
            val unknownServiceName = "noSuchPersistentService"
            val data = createServiceData()

            val exception = shouldThrow<IllegalArgumentException> {
                data.getPersistentService(unknownServiceName)
            }

            exception.message shouldContain unknownServiceName
        }
    }

    "contains" should {
        "return true for an existing service" {
            val data = createServiceData()

            (service1.name in data) shouldBe true
        }

        "return false for a non-existing service" {
            val data = createServiceData()

            ("nonExistingService" in data) shouldBe false
        }
    }

    "addService" should {
        "return a new instance with an added service" {
            val newService = PersistentService(
                serviceDefinition = ServiceDefinition(
                    name = "testService3",
                    multicastAddress = "231.10.3.44",
                    port = 7003,
                    requestCode = "code3"
                ),
                lookupTimeout = 11.seconds,
                sendRequestInterval = 44.milliseconds
            )
            val data = createServiceData()

            val newData = data.addService(newService)

            newData.services[2] shouldBe newService
        }

        "throw an exception if there is a service with the given name" {
            val data = createServiceData()

            val exception = shouldThrow<IllegalArgumentException> {
                data.addService(persistentService1)
            }
            exception.message shouldContain service1.name
        }
    }

    "removeService" should {
        "remove the service with the given name" {
            val data = createServiceData()

            val newData = data.removeService(service2.name)

            newData.services shouldContainExactly listOf(persistentService1)
        }

        "throw an exception if the service does not exist" {
            val nonExistingService = "ThisServiceDoesNotExist"
            val data = createServiceData()

            val exception = shouldThrow<IllegalArgumentException> {
                data.removeService(nonExistingService)
            }
            exception.message shouldContain nonExistingService
        }
    }

    "updateService" should {
        "update a service in place" {
            val updatedService = persistentService1.copy(
                serviceDefinition = service1.copy(port = 8001),
                sendRequestInterval = 222.milliseconds
            )
            val data = createServiceData()

            val newData = data.updateService(updatedService.serviceDefinition.name, updatedService)

            newData.services shouldContainExactly listOf(updatedService, persistentService2)
        }

        "throw an exception if no service with this name exists" {
            val unknownService = "nonExistingService"
            val updatedService = persistentService1.copy(
                serviceDefinition = service1.copy(port = 65432)
            )
            val data = createServiceData()

            val exception = shouldThrow<IllegalArgumentException> {
                data.updateService(unknownService, updatedService)
            }
            exception.message shouldContain unknownService
        }

        "allow renaming a service" {
            val newServiceName = "renamedTestService"
            val updatedService = persistentService1.copy(
                serviceDefinition = service1.copy(name = newServiceName)
            )
            val data = createServiceData()

            val newData = data.updateService(service1.name, updatedService)
            newData.services shouldContainExactly listOf(updatedService, persistentService2)
        }

        "throw an exception if the new name of a service is already in use" {
            val updatedService = persistentService1.copy(
                serviceDefinition = service1.copy(name = service2.name, port = 65432)
            )
            val data = createServiceData()

            val exception = shouldThrow<IllegalArgumentException> {
                data.updateService(service1.name, updatedService)
            }
            exception.message shouldContain service2.name
        }
    }

    "moveUp" should {
        "move a service one position upwards" {
            val data = createServiceData()

            val newData = data.moveUp(service2.name)

            newData.services shouldContainExactly listOf(persistentService2, persistentService1)
        }

        "throw an exception if no service with this name exists" {
            val nonExistingService = "AServiceThatDoesNotExist"
            val data = createServiceData()

            val exception = shouldThrow<IllegalArgumentException> {
                data.moveUp(nonExistingService)
            }
            exception.message shouldContain nonExistingService
        }

        "return the same instance if the service is already the first one" {
            val data = createServiceData()

            val newData = data.moveUp(service1.name)

            newData should beTheSameInstanceAs(data)
        }
    }

    "moveDown" should {
        "move a service one position downwards" {
            val data = createServiceData()

            val newData = data.moveDown(service1.name)

            newData.services shouldContainExactly listOf(persistentService2, persistentService1)
        }

        "throw an exception if no service with this name exists" {
            val nonExistingService = "AServiceThatDoesNotExist"
            val data = createServiceData()

            val exception = shouldThrow<IllegalArgumentException> {
                data.moveDown(nonExistingService)
            }
            exception.message shouldContain nonExistingService
        }

        "return the same instance if the service is already the last one" {
            val data = createServiceData()

            val newData = data.moveDown(service2.name)

            newData should beTheSameInstanceAs(data)
        }
    }

    "getPreviousAndNext" should {
        "return a fully defined pair if a previous and a next service exist" {
            val service3 = service2.copy(name = "testService3")
            val persistentService3 = persistentService2.copy(serviceDefinition = service3)

            val data = ServiceData(listOf(persistentService1, persistentService2, persistentService3))
            val (previous, next) = data.getPreviousAndNext(service2.name)

            previous shouldBe service1.name
            next shouldBe service3.name
        }

        "return null as previous service for the first service" {
            val data = createServiceData()

            val (previous, next) = data.getPreviousAndNext(service1.name)

            previous should beNull()
            next shouldBe service2.name
        }

        "return null as next service for the last service" {
            val data = createServiceData()

            val (previous, next) = data.getPreviousAndNext(service2.name)

            previous shouldBe service1.name
            next should beNull()
        }

        "return a pair with null values for the only service in the data object" {
            val data = ServiceData(listOf(persistentService1))

            val (previous, next) = data.getPreviousAndNext(service1.name)

            previous should beNull()
            next should beNull()
        }

        "throw an exception if the service name cannot be resolved" {
            val serviceName = "nonExistingService"
            val data = createServiceData()

            val exception = shouldThrow<IllegalArgumentException> {
                data.getPreviousAndNext(serviceName)
            }

            exception.message shouldContain serviceName
        }
    }
})

/** A test service definition. */
private val service1 = ServiceDefinition(
    name = "testService1",
    multicastAddress = "231.10.1.42",
    port = 7001,
    requestCode = "code1"
)

/** Another test service definition. */
private val service2 = ServiceDefinition(
    name = "testService2",
    multicastAddress = "231.10.2.43",
    port = 7002,
    requestCode = "code2"
)

/** A test persistent service. */
private val persistentService1 = PersistentService(
    serviceDefinition = service1,
    lookupTimeout = 53.seconds,
    sendRequestInterval = 111.milliseconds
)

/** Another test persistent service. */
private val persistentService2 = PersistentService(
    serviceDefinition = service2,
    lookupTimeout = null,
    sendRequestInterval = null
)

/**
 * Create a [ServiceData] instance with test services.
 */
private fun createServiceData(): ServiceData =
    ServiceData(services = listOf(persistentService1, persistentService2))
