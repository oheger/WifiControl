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

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument

/**
 * An object defining the navigation routes and their parameters used by this app.
 */
object Navigation {
    /** Route path for the services overview UI. */
    private const val ROUTE_SERVICES = "services"

    /** The argument defining the service index. */
    private const val ARG_SERVICE_INDEX = "serviceIndex"

    /**
     * Generate a route path from this string with the given sub [path].
     */
    private fun String.subPath(path: String) = "$this/$path"

    /**
     * Generate a route path from this string with a sub path defined by the given [argument].
     */
    private fun String.subPathArgument(argument: String) = subPath("{$argument}")

    /**
     * A data class defining the arguments for the service details UI.
     */
    data class ServiceDetailsArgs(
        /** The index of the service to be displayed. */
        val serviceIndex: Int
    )

    /**
     * A base class representing a navigation route supported by this app.
     */
    sealed class NavigationRoute(
        /** The string representation of this route path. */
        val route: String,

        /** A list with the arguments used in this route. */
        val arguments: List<NamedNavArgument> = emptyList()
    )

    /**
     * The object representing the route to the services overview. This is static and does not support any arguments.
     */
    data object ServicesRoute : NavigationRoute(ROUTE_SERVICES)

    /**
     * The object representing the route to the details of a service. The service in question is defined by an
     * argument that contains the index of the service in the global service data object.
     */
    data object ServiceDetailsRoute : NavigationRoute(
        ROUTE_SERVICES.subPathArgument(ARG_SERVICE_INDEX),
        listOf(
            navArgument(ARG_SERVICE_INDEX) {
                type = NavType.IntType
            }
        )
    ) {
        /**
         * Generate the routing path to the service details UI for the given [arguments].
         */
        fun forArguments(arguments: ServiceDetailsArgs): String =
            ROUTE_SERVICES.subPath(arguments.serviceIndex.toString())

        /**
         * Extract a [ServiceDetailsArgs] object from the given [entry].
         */
        fun fromEntry(entry: NavBackStackEntry): ServiceDetailsArgs =
            ServiceDetailsArgs(entry.arguments?.getInt(ARG_SERVICE_INDEX) ?: 0)
    }
}
