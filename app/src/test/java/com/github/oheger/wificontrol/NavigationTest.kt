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
package com.github.oheger.wificontrol

import android.os.Bundle

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

import io.mockk.every
import io.mockk.mockk

class NavigationTest : WordSpec({
    "ServicesRoute" should {
        "define the correct route" {
            Navigation.ServicesRoute.route shouldBe "services"
        }

        "have no navigation arguments" {
            Navigation.ServicesRoute.arguments should beEmpty()
        }
    }

    "ServiceDetailsRoute" should {
        "define the correct route" {
            Navigation.ServiceDetailsRoute.route shouldBe "services/{serviceIndex}"
        }

        "have a correct argument" {
            Navigation.ServiceDetailsRoute.arguments shouldHaveSize 1

            with(Navigation.ServiceDetailsRoute.arguments.single()) {
                name shouldBe "serviceIndex"
                argument.type shouldBe NavType.IntType
            }
        }

        "generate the correct route for a specific service" {
            val detailsArgs = Navigation.ServiceDetailsArgs(42)

            val route = Navigation.ServiceDetailsRoute.forArguments(detailsArgs)

            route shouldBe "services/42"
        }

        "return a ServiceDetailsArg object from a NavBackStackEntry" {
            val serviceIndex = 12

            val bundle = mockk<Bundle> {
                every { getInt("serviceIndex") } returns serviceIndex
            }
            val entry = mockk<NavBackStackEntry> {
                every { arguments } returns bundle
            }

            val args = Navigation.ServiceDetailsRoute.fromEntry(entry)

            args shouldBe Navigation.ServiceDetailsArgs(serviceIndex)
        }

        "return a ServiceDetailsArg object from a NavBackStackEntry if the Bundle is null" {
            val entry = mockk<NavBackStackEntry> {
                every { arguments } returns null
            }

            val args = Navigation.ServiceDetailsRoute.fromEntry(entry)

            args shouldBe Navigation.ServiceDetailsArgs(0)
        }
    }

    "ControlServiceRoute" should {
        "define the correct route" {
            Navigation.ControlServiceRoute.route shouldBe "control/{serviceName}"
        }

        "have a correct argument" {
            with(Navigation.ControlServiceRoute.arguments.single()) {
                name shouldBe "serviceName"
                argument.type shouldBe NavType.StringType
                argument.isNullable shouldBe false
            }
        }

        "generate the correct route for a specific service name" {
            val controlArgs = Navigation.ControlServiceArgs("MyServiceToControl")

            val route = Navigation.ControlServiceRoute.forArguments(controlArgs)

            route shouldBe "control/MyServiceToControl"
        }

        "return a ControlServiceArgs object from a NavBackStackEntry" {
            val serviceName = "ControlTestService"

            val bundle = mockk<Bundle> {
                every { getString("serviceName") } returns serviceName
            }
            val entry = mockk<NavBackStackEntry> {
                every { arguments } returns bundle
            }

            val args = Navigation.ControlServiceRoute.fromEntry(entry)

            args shouldBe Navigation.ControlServiceArgs(serviceName)
        }

        "return a ControlServiceArgs object from a NavBackStackEntry if the Bundle is null" {
            val entry = mockk<NavBackStackEntry> {
                every { arguments } returns null
            }

            val args = Navigation.ControlServiceRoute.fromEntry(entry)

            args shouldBe Navigation.ControlServiceArgs("")
        }

        "URL encode special characters in a service name when generating the route" {
            val serviceName = "My Test? Service"
            val expectedRoute = "control/My+Test%3F+Service"

            val route = Navigation.ControlServiceRoute.forArguments(Navigation.ControlServiceArgs(serviceName))

            route shouldBe expectedRoute
        }

        "URL decode the service name from a NavBackStackEntry" {
            val encodedServiceName = "Control+Test%23+Service"
            val serviceName = "Control Test# Service"

            val bundle = mockk<Bundle> {
                every { getString("serviceName") } returns encodedServiceName
            }
            val entry = mockk<NavBackStackEntry> {
                every { arguments } returns bundle
            }

            val args = Navigation.ControlServiceRoute.fromEntry(entry)

            args shouldBe Navigation.ControlServiceArgs(serviceName)
        }
    }
})
