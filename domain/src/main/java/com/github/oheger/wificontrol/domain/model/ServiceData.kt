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
 * A data class collecting information about all services managed by this app. It is used to persist the state of the
 * app. It also provides functions to manipulate the state.
 */
data class ServiceData(
    /** A list with the persistent services managed by this class. */
    val services: List<PersistentService>
) {
    companion object {
        /**
         * A special index value to indicate a new service. Using this value means that not an existing service from
         * the list of a [ServiceData] instance is referenced, but a one that will be newly created. This constant is
         * used from multiple use cases.
         */
        const val NEW_SERVICE_INDEX = -1
    }

    /**
     * Return a [LookupService] object for the managed service at the given [index].
     */
    operator fun get(index: Int): LookupService = with(services[index]) {
        val lookupConfig = LookupConfig(
            lookupTimeout ?: LookupConfig.DEFAULT_LOOKUP_TIMEOUT,
            sendRequestInterval ?: LookupConfig.DEFAULT_SEND_REQUEST_INTERVAL
        )
        LookupService(serviceDefinition, lookupConfig)
    }

    /**
     * Return a [LookupService] that matches the given [serviceName] or *null* if no such service exists.
     */
    operator fun get(serviceName: String): LookupService? =
        indexOf(serviceName).takeIf { it >= 0 }?.let { get(it) }

    /**
     * Check whether this object contains a service with the given [serviceName].
     */
    operator fun contains(serviceName: String): Boolean =
        indexOf(serviceName) >= 0

    /**
     * Return the [LookupService] with the given [serviceName] or throw an [IllegalArgumentException] if the name
     * cannot be resolved.
     */
    fun getService(serviceName: String): LookupService =
        get(indexOfOrThrow(serviceName))

    /**
     * Return the [PersistentService] with the given [serviceName] or throw an [IllegalArgumentException] if the
     * name cannot be resolved.
     */
    fun getPersistentService(serviceName: String): PersistentService =
        services[indexOfOrThrow(serviceName)]

    /**
     * Return a [Pair] with the names of previous and next services in the list for the service with the given
     * [serviceName] (if they exist). This is useful for a quick navigation through the ordered list of services.
     * In the special case that the [serviceName] cannot be resolved, an [IllegalArgumentException] is thrown.
     */
    fun getPreviousAndNext(serviceName: String): Pair<String?, String?> {
        val serviceIndex = indexOfOrThrow(serviceName)
        val previous = (serviceIndex - 1).takeIf { it >= 0 }?.let(this::get)?.service?.name
        val next = (serviceIndex + 1).takeIf { it < services.size }?.let(this::get)?.service?.name

        return previous to next
    }

    /**
     * Return a new [ServiceData] instance with the given [service] added as the last element in the list of services.
     * Throw an [IllegalArgumentException] if there is already a service with the given name.
     */
    fun addService(service: PersistentService): ServiceData {
        require(service.serviceDefinition.name !in this) {
            "A service with the name '${service.serviceDefinition.name}' already exists."
        }

        return copy(services = services + service)
    }

    /**
     * Return a new [ServiceData] instance that does no longer contain the [PersistentService] with the given
     * [serviceName]. Throw an [IllegalArgumentException] if no such service exists.
     */
    fun removeService(serviceName: String): ServiceData {
        val newServices = services.filterNot { it.serviceDefinition.name == serviceName }
        require(newServices.size < services.size) {
            "A service with the name '$serviceName' does not exist."
        }

        return copy(services = newServices)
    }

    /**
     * Return a new [ServiceData] instance that contains the given [service] as replacement of a service with the
     * given [originalServiceName]. Store this service at the same index as it is located now. Throw an
     * [IllegalArgumentException] if no service with this name exists. The [originalServiceName] must be specified in
     * case the service has been renamed. In this case, an [IllegalArgumentException] is thrown as well if the new
     * name is already used by another service.
     */
    fun updateService(originalServiceName: String, service: PersistentService): ServiceData {
        val pos = indexOfOrThrow(originalServiceName)
        if (service.serviceDefinition.name != originalServiceName) {
            require(indexOf(service.serviceDefinition.name) < 0) {
                "Service cannot be renamed. " +
                        "There is already a service with the name '${service.serviceDefinition.name}'."
            }
        }

        val newServices = services.toMutableList()
        newServices[pos] = service
        return copy(services = newServices)
    }

    /**
     * Return a new [ServiceData] instance that has the service with the given name moved upwards by one position.
     * If the service is already at the top, return the same instance. Throw an [IllegalArgumentException] if no
     * service with this name exists.
     */
    fun moveUp(serviceName: String): ServiceData =
        indexOfOrThrow(serviceName).takeIf { it > 0 }?.let { pos ->
            swapServices(pos, pos - 1)
        } ?: this

    /**
     * Return a new [ServiceData] instance that has the service with the given name moved down by one position.
     * If the service is already the last in the list, return the same instance. Throw an [IllegalArgumentException]
     * if no service with this name exists.
     */
    fun moveDown(serviceName: String): ServiceData =
        indexOfOrThrow(serviceName).takeIf { it < services.size - 1 }?.let { pos ->
            swapServices(pos, pos + 1)
        } ?: this

    /**
     * Return a new instance of [ServiceData] in which the services at the given positions [pos1] and [pos2] are
     * swapped.
     */
    private fun swapServices(pos1: Int, pos2: Int): ServiceData {
        val newServices = services.toMutableList()
        val help = newServices[pos2]
        newServices[pos2] = newServices[pos1]
        newServices[pos1] = help
        return copy(services = newServices)
    }

    /**
     * Return the index of the service with the given [serviceName] or -1 if the service does not exist.
     */
    private fun indexOf(serviceName: String): Int =
        services.indexOfFirst { it.serviceDefinition.name == serviceName }

    /**
     * Return the index of the service with the given [serviceName] or throw an [IllegalArgumentException] if no
     * service with this name exists.
     */
    private fun indexOfOrThrow(serviceName: String): Int =
        indexOf(serviceName).takeIf { it >= 0 }
            ?: throw IllegalArgumentException("A service with the name '$serviceName' does not exist.")
}
