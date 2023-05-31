/*
 * Copyright 2023 Oliver Heger.
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

/**
 * Root of a class hierarchy to represent the current state of looking up the server in the network.
 *
 * Based on this state, the UI can be updated accordingly. When the URL to the server has been finally found, the
 * lookup process is done.
 */
sealed interface ServerLookupState

/**
 * An object representing the state that the Wi-Fi status needs to be checked. This is the initial state of the
 * lookup process. A connected Wi-Fi is the prerequisite for searching for the server in the network.
 */
object NetworkStatusUnknown : ServerLookupState

/**
 * An object representing the state that no Wi-Fi is available. So, before continuing the lookup, the process has to
 * wait for a Wi-Fi connection.
 */
object WiFiUnavailable : ServerLookupState

/**
 * An object representing the state that Wi-Fi is available, and the network is now searched for the HTTP service in
 * question. This is done by sending a multicast UDP request periodically, until a successful response is received.
 */
object SearchingInWiFi : ServerLookupState

/**
 * A data class representing the state that the server could be located on the network. Its URL is now available, so
 * interaction with it is now possible.
 */
data class ServerFound(
    /**
     * The URL under which the server can be reached. This was obtained from the UDP response that was received.
     */
    val uri: String
) : ServerLookupState
