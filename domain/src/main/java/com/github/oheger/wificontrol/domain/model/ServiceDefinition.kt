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

import java.net.InetAddress

/**
 * An enumeration class defining the mechanism how to obtain the address of a service. This is evaluated by the
 * control UI to find the URL of the service's index page.
 */
enum class ServiceAddressMode {
    /**
     * The URL of the service is obtained using a discovery operation in the WiFi network.
     */
    WIFI_DISCOVERY,

    /**
     * The URL is provided as part of the service properties. It can be used directly to contact the service.
     */
    FIX_URL
}

/**
 * A data class defining a service to be controlled by this application.
 *
 * This class assigns a name to the service and defines the mechanism how to obtain the service address (i.e. the
 * URL pointing to its graphical user interface). Depending on this mechanism, different properties are required.
 * The class stores all of them, since it is possible that multiple methods are defined, and the user can switch
 * between those. However, only the properties that belong to the currently selected method are guaranteed to be
 * valid.
 */
data class ServiceDefinition(
    /**
     * A name for this service. This can be chosen freely by the user. It is used to distinguish between multiple
     * services that can be controlled via this application. Service names must be unique.
     */
    val name: String,

    /**
     * The mode defining how the address of this service needs to be obtained.
     */
    val addressMode: ServiceAddressMode,

    /**
     * The multicast address to which UDP requests need to be sent in order to discover the service. Used in
     * [ServiceAddressMode.WIFI_DISCOVERY].
     */
    val multicastAddress: String,

    /**
     * The port to send UDP multicast requests to. Used in [ServiceAddressMode.WIFI_DISCOVERY].
     */
    val port: Int,

    /**
     * A code that becomes the payload of UDP requests. It is evaluated by the service. Only if the code matches, the
     * service sends a response. Used in [ServiceAddressMode.WIFI_DISCOVERY]
     */
    val requestCode: String,

    /**
     * A fix URL provided by the user under which the service can be reached. Used in [ServiceAddressMode.FIX_URL].
     */
    val serviceUrl: String
) {
    /**
     * The multicast address to which UDP requests need to be sent as an [InetAddress]. This may only be used in mode
     * [ServiceAddressMode.WIFI_DISCOVERY].
     */
    val multicastInetAddress: InetAddress by lazy { InetAddress.getByName(multicastAddress) }
}
