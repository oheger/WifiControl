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
                persistentService1.networkTimeout!!,
                persistentService1.retryDelay!!,
                persistentService1.sendRequestInterval!!
            )
            val data = createServiceData()

            val lookupService = data[0]

            lookupService.service shouldBe service1
            lookupService.lookupConfig shouldBe expectedLookupConfig
        }

        "return a LookupService with default lookup properties" {
            val expectedLookupConfig = LookupConfig(
                LookupConfig.DEFAULT_TIMEOUT,
                LookupConfig.DEFAULT_RETRY_DELAY,
                LookupConfig.DEFAULT_SEND_REQUEST_INTERVAL
            )
            val data = createServiceData()

            val lookupService = data[1]

            lookupService.service shouldBe service2
            lookupService.lookupConfig shouldBe expectedLookupConfig
        }
    }

    "current" should {
        "return null if the selected index is larger than the number of services" {
            val data = ServiceData(emptyList(), 0)

            data.current should beNull()
        }

        "return null if the selected index is less than zero" {
            val data = createServiceData(current = -1)

            data.current should beNull()
        }

        "return the service at the current index" {
            val data = createServiceData(1)

            data.current shouldBe data[1]
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
                networkTimeout = 11.seconds,
                retryDelay = null,
                sendRequestInterval = 44.milliseconds
            )
            val data = createServiceData(current = 1)

            val newData = data.addService(newService)

            newData.services[2] shouldBe newService
            newData.currentIndex shouldBe data.currentIndex
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

            newData.currentIndex shouldBe data.currentIndex
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

        "reset the current index if the current service is removed" {
            val data = createServiceData(current = 1)

            val newData = data.removeService(service2.name)

            newData.currentIndex shouldBe 0
        }
    }

    "updateService" should {
        "update a service in place" {
            val updatedService = persistentService1.copy(
                serviceDefinition = service1.copy(port = 8001),
                sendRequestInterval = 222.milliseconds
            )
            val data = createServiceData()

            val newData = data.updateService(updatedService)

            newData.services shouldContainExactly listOf(updatedService, persistentService2)
        }

        "throw an exception if no service with this name exists" {
            val updatedService = persistentService1.copy(
                serviceDefinition = service1.copy(name = "nonExistingService")
            )
            val data = createServiceData()

            val exception = shouldThrow<IllegalArgumentException> {
                data.updateService(updatedService)
            }
            exception.message shouldContain updatedService.serviceDefinition.name
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

    "makeCurrent" should {
        "correctly set the current index" {
            val data = createServiceData(current = 1)

            val newData = data.makeCurrent(service1.name)

            newData.currentIndex shouldBe 0
        }

        "throw an exception if no service with this name exists" {
            val nonExistingService = "thisServiceCannotBeTheCurrentOne"
            val data = createServiceData()

            val exception = shouldThrow<IllegalArgumentException> {
                data.makeCurrent(nonExistingService)
            }
            exception.message shouldContain nonExistingService
        }

        "return the same instance if the service is already the current one" {
            val data = createServiceData(current = 0)

            val newData = data.makeCurrent(service1.name)

            newData should beTheSameInstanceAs(data)
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
    networkTimeout = 53.seconds,
    retryDelay = 28.seconds,
    sendRequestInterval = 111.milliseconds
)

/** Another test persistent service. */
private val persistentService2 = PersistentService(
    serviceDefinition = service2,
    networkTimeout = null,
    retryDelay = null,
    sendRequestInterval = null
)

/**
 * Create a [ServiceData] instance with the test services and the given [current] index.
 */
private fun createServiceData(current: Int = 0): ServiceData =
    ServiceData(
        services = listOf(persistentService1, persistentService2),
        currentIndex = current
    )
