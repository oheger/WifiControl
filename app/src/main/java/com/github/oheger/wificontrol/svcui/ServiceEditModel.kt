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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import com.github.oheger.wificontrol.domain.model.PersistentService
import com.github.oheger.wificontrol.domain.model.ServiceDefinition

/**
 * A helper class that stores information about the properties of a service that is currently edited.
 *
 * The goal of this class is to provide sufficient information for an editor UI. This includes all the properties of
 * a service, but also information if the current values are valid or if there are validation failures. For a new
 * service, the properties are initially invalid (since they are empty). However, the UI should not display lots of
 * errors initially. Therefore, a property is considered invalid only after it has been changed or after a full
 * validation.
 */
internal class ServiceEditModel(
    /** The service to be edited using this model. */
    service: PersistentService
) {
    companion object {
        /** Regular expression to validate an IP address. */
        private val regIpAddress = Regex("""(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})""")

        /**
         * Return a flag whether the given mandatory [string][s] is valid.
         */
        internal fun validateRequiredString(s: String): Boolean =
            s.isNotBlank() && s == s.trim()

        /**
         * Return a flag whether the given [address] is a valid multicast IP address.
         */
        internal fun validateMulticastAddress(address: String): Boolean =
            regIpAddress.matchEntire(address)?.let { result ->
                result.groups.drop(1).all { component ->
                    val componentInt = component?.value?.toInt() ?: -1
                    componentInt in 0..255
                }
            } == true

        /**
         * Return a flag whether the given [port] is a valid port number.
         */
        internal fun validatePort(port: String): Boolean =
            runCatching { port.toInt() in 0..65535 }.getOrDefault(false)
    }

    /** Stores the name of the service. */
    private var serviceNameField by mutableStateOf(service.serviceDefinition.name)

    /** Stores the result of the service name validation. */
    private var serviceNameValidationResult by mutableStateOf(validateRequiredString(serviceNameField))

    /** Stores a flag whether the service name property has already been edited. */
    private var serviceNameEdited by mutableStateOf(false)

    /** The name of the edited service. */
    var serviceName: String
        get() = serviceNameField
        set(value) {
            serviceNameField = value
            serviceNameEdited = true
            serviceNameValidationResult = validateRequiredString(value)
        }

    /** Flag whether the service name is considered valid. */
    val serviceNameValid: Boolean
        get() = serviceNameValidationResult || !serviceNameEdited

    /** Stores the multicast lookup address of the service. */
    private var multicastAddressField by mutableStateOf(service.serviceDefinition.multicastAddress)

    /** Stores the result of the multicast address validation. */
    private var multicastAddressValidationResult by mutableStateOf(validateMulticastAddress(multicastAddressField))

    /** Stores a flag whether the multicast address property has already been edited. */
    private var multicastAddressEdited by mutableStateOf(false)

    /** Property for the multicast lookup address of the service. */
    var multicastAddress: String
        get() = multicastAddressField
        set(value) {
            multicastAddressField = value
            multicastAddressEdited = true
            multicastAddressValidationResult = validateMulticastAddress(value)
        }

    /** Flag whether the multicast address is considered valid. */
    val multicastAddressValid: Boolean
        get() = multicastAddressValidationResult || !multicastAddressEdited

    /** Stores the port of the service. */
    private var portField by mutableStateOf(service.serviceDefinition.port.toString())

    /** Stores the result of the port validation. */
    private var portValidationResult by mutableStateOf(validatePort(portField))

    /** Stores a flag whether the port property has already been edited. */
    private var portEdited by mutableStateOf(false)

    /** Property for the port of the service. */
    var port: String
        get() = portField
        set(value) {
            portField = value
            portEdited = true
            portValidationResult = validatePort(value)
        }

    /** Flag whether the port is considered valid. */
    val portValid: Boolean
        get() = portValidationResult || !portEdited

    /** Stores the request code for the service. */
    private var codeField by mutableStateOf(service.serviceDefinition.requestCode)

    /** Stores the result of the request code validation. */
    private var codeValidationResult by mutableStateOf(validateRequiredString(codeField))

    /** Stores a flag whether the request code property has already been edited. */
    private var codeEdited by mutableStateOf(false)

    /** Property for the request code of the service. */
    var code: String
        get() = codeField
        set(value) {
            codeField = value
            codeEdited = true
            codeValidationResult = validateRequiredString(value)
        }

    /** Flag whether the code is considered valid. */
    val codeValid: Boolean
        get() = codeValidationResult || !codeEdited

    /**
     * Perform a full validation of all edit fields and return a flag with the result. After calling this function,
     * all fields are marked as edited, so that the _Valid_ properties are correctly initialized. Only if this
     * function returns *true*, the edit operation can be successfully completed. If the result is *false*, the UI
     * can update itself and mark the invalid fields accordingly.
     */
    fun validate(): Boolean {
        serviceNameEdited = true
        multicastAddressEdited = true
        portEdited = true
        codeEdited = true

        return serviceNameValidationResult && multicastAddressValidationResult && portValidationResult &&
                codeValidationResult
    }

    /**
     * Return a [PersistentService] instance with properties that correspond to the current values in the edit fields.
     * This function can be called only after [validate] has returned *true*.
     */
    fun editedService(): PersistentService =
        PersistentService(
            serviceDefinition = ServiceDefinition(
                name = serviceName,
                multicastAddress = multicastAddress,
                port = port.toInt(),
                requestCode = code
            ),
            lookupTimeout = null,
            sendRequestInterval = null
        )
}