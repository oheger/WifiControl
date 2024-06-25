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

/**
 * The root of a class hierarchy that represents the service that is currently controlled by the app. This data is
 * persisted, so that on reopening the app, this service can directly be loaded and displayed. There are two subtypes
 * of this interface: one for a defined current service, and one for an undefined one. The latter is used when the
 * app was closed and another screen than a service control UI was active.
 */
sealed interface CurrentService {
    companion object {
        /**
         * Return a [CurrentService] instance based on the provided [serviceName]. The correct subtype is chosen based
         * on the fact whether the name is *null* or not.
         */
        fun forServiceName(serviceName: String?): CurrentService =
            serviceName?.let(::DefinedCurrentService) ?: UndefinedCurrentService
    }
}

/**
 * A data class representing a defined current service. An instance holds the name of this service.
 */
data class DefinedCurrentService(
    /** The name of the current service. */
    val serviceName: String
) : CurrentService

/**
 * An object to represent the state that no service is currently controlled by this app.
 */
data object UndefinedCurrentService : CurrentService
