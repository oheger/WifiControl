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
 * A data class defining a service to be looked up in the Wi-Fi network and to be controlled by this application.
 *
 * This class assigns a name to the service and holds the properties required to do a discovery in the network.
 */
data class ServiceDefinition(
    /**
     * A name for this service. This can be chosen freely by the user. It is used to distinguish between multiple
     * services that can be controlled via this application. Service names must be unique.
     */
    val name: String,

    /** The multicast address to which UDP requests need to be sent in order to discover the service. */
    val multicastAddress: String,

    /** The port to send UDP multicast requests to. */
    val port: Int,

    /**
     * A code that becomes the payload of UDP requests. It is evaluated by the service. Only if the code matches, the
     * service sends a response.
     */
    val requestCode: String
) {
    /** The multicast address to which UDP requests need to be sent as an [InetAddress]. */
    val multicastInetAddress: InetAddress by lazy { InetAddress.getByName(multicastAddress) }
}
