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
import com.github.oheger.wificontrol.domain.model.ServiceAddressMode
import com.github.oheger.wificontrol.domain.model.ServiceDefinition

import java.net.URI

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A helper class that stores information about the properties of a service that is currently edited.
 *
 * The goal of this class is to provide sufficient information for an editor UI. This includes all the properties of
 * a service, but also information if the current values are valid or if there are validation failures. For a new
 * service, the properties are initially invalid (since they are empty). However, the UI should not display lots of
 * errors initially. Therefore, a property is considered invalid only after it has been changed or after a full
 * validation.
 *
 * An instance of this class lives in the view model of the UI which controls its life-cycle.
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
            validateNumber(port) { it in 0..65535 }

        /**
         * Return a flag whether the given [duration] in string form is valid.
         */
        internal fun validateDuration(duration: String): Boolean =
            validateNumber(duration) { it > 0 }

        /**
         * Perform a validation of a [string][s] that should be a number and adhere to constraints expressed by the
         * given [check] function.
         */
        private fun validateNumber(s: String, check: (Int) -> Boolean): Boolean =
            runCatching { check(s.toInt()) }.getOrDefault(false)

        /**
         * Perform a validation of a [string][s] that should be a URL.
         */
        private fun validateUrl(s: String): Boolean =
            validateRequiredString(s) && runCatching { URI.create(s) }.isSuccess
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

    /** Stores the lookup timeout in seconds. */
    private var lookupTimeoutField by mutableStateOf(service.lookupTimeout?.inWholeSeconds?.toString().orEmpty())

    /** Stores the result of the lookup timeout validation. */
    private var lookupTimeoutValidationResult by mutableStateOf(validateDuration(lookupTimeoutField))

    /** Stores a flag whether the lookup timeout property has already been edited. */
    private var lookupTimeoutEdited by mutableStateOf(false)

    /** Property for the lookup timeout for service discovery. */
    var lookupTimeoutSec: String
        get() = lookupTimeoutField
        set(value) {
            lookupTimeoutField = value
            lookupTimeoutEdited = true
            lookupTimeoutValidationResult = validateDuration(value)
        }

    /** Flag that determines whether the default lookup timeout value should be used for this service. */
    var lookupTimeoutDefault by mutableStateOf(service.lookupTimeout == null)

    /** Flag whether the lookup timeout is considered valid. */
    val lookupTimeoutValid: Boolean
        get() = lookupTimeoutValidationResult || lookupTimeoutDefault || !lookupTimeoutEdited

    /** Stores the send request interval in milliseconds. */
    private var sendRequestIntervalField by mutableStateOf(
        service.sendRequestInterval?.inWholeMilliseconds?.toString().orEmpty()
    )

    /** Stores the result of the send request interval validation. */
    private var sendRequestIntervalValidationResult by mutableStateOf(validateDuration(sendRequestIntervalField))

    /** Stores a flag whether the send request interval property has already been edited. */
    private var sendRequestIntervalEdited by mutableStateOf(false)

    /** Property for the request interval for service discovery. */
    var sendRequestIntervalMs: String
        get() = sendRequestIntervalField
        set(value) {
            sendRequestIntervalField = value
            sendRequestIntervalEdited = true
            sendRequestIntervalValidationResult = validateDuration(value)
        }

    /** Flag that determines whether the default send request interval value should be used for this service. */
    var sendRequestIntervalDefault by mutableStateOf(service.sendRequestInterval == null)

    /** Flag whether the send request interval is considered valid. */
    val sendRequestIntervalValid: Boolean
        get() = sendRequestIntervalValidationResult || sendRequestIntervalDefault || !sendRequestIntervalEdited

    /**
     * Flag that determines whether a URL is provided for the current service. Based on this flag, the properties
     * displayed in the URL are changed, and it impacts also the validation.
     */
    var isServiceUrlProvided by mutableStateOf(service.serviceDefinition.addressMode == ServiceAddressMode.FIX_URL)

    /** Stores the (provided) URL of the service. */
    private var serviceUrlField by mutableStateOf(service.serviceDefinition.serviceUrl)

    /** Stores the result of the validation of the service URL field. */
    private var serviceUrlValidationResult by mutableStateOf(validateUrl(serviceUrlField))

    /** Stores a flag whether the service URL property has already been edited. */
    private var serviceUrlEdited by mutableStateOf(false)

    /** Property for the (provided) service URL. */
    var serviceUrl: String
        get() = serviceUrlField
        set(value) {
            serviceUrlField = value
            serviceUrlEdited = true
            serviceUrlValidationResult = validateUrl(value)
        }

    /** Flag whether the service URL is considered valid. */
    val serviceUrlValid: Boolean
        get() = serviceUrlValidationResult || !serviceUrlEdited

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
        lookupTimeoutEdited = true
        sendRequestIntervalEdited = true
        serviceUrlEdited = true

        return serviceNameValidationResult && validateAddress()
    }

    /**
     * Return a [PersistentService] instance with properties that correspond to the current values in the edit fields.
     * This function can be called only after [validate] has returned *true*.
     */
    fun editedService(): PersistentService =
        PersistentService(
            serviceDefinition = ServiceDefinition(
                name = serviceName,
                addressMode = addressMode(),
                multicastAddress = multicastAddress,
                port = port.toInt(),
                requestCode = code,
                serviceUrl = serviceUrl
            ),
            lookupTimeout = if (lookupTimeoutDefault) null else lookupTimeoutSec.toInt().seconds,
            sendRequestInterval = if (sendRequestIntervalDefault) null else sendRequestIntervalMs.toInt().milliseconds
        )

    /**
     * Determine the [ServiceAddressMode] for the edited service based on the flag whether a URL is provided.
     */
    private fun addressMode(): ServiceAddressMode =
        if (isServiceUrlProvided) ServiceAddressMode.FIX_URL
        else ServiceAddressMode.WIFI_DISCOVERY

    /**
     * Perform a validation of the properties that define how the service address needs to be obtained. Which
     * properties are affected is determined by the flag whether a URL is provided.
     */
    private fun validateAddress(): Boolean =
        if (isServiceUrlProvided) validateFixUrl()
        else validateDiscovery()

    /**
     * Return a flag whether the properties defining the service discovery are all valid.
     */
    private fun validateDiscovery(): Boolean =
        multicastAddressValidationResult && portValidationResult && codeValidationResult && lookupTimeoutValid &&
                sendRequestIntervalValid

    /**
     * Return a flag whether the properties defining the FIX_URL address mode are all valid.
     */
    private fun validateFixUrl(): Boolean =
        serviceUrlValidationResult
}
