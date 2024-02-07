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

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A data class defining a number of properties that control the lookup operation for a service in the local
 * Wi-Fi. These properties are taken into account when the app sends multicast requests to locate a service in
 * the network.
 */
data class LookupConfig(
    /**
     * A timeout after which a test is considered a failure. It applies to multiple checks that wait for some kind of
     * response.
     */
    val networkTimeout: Duration,

    /**
     * A delay after which another attempt is made to contact the service after a failure or when no response was
     * received.
     */
    val retryDelay: Duration,

    /**
     * The interval in which requests are sent to the UDP server. Since packets can get lost, or there could be race
     * conditions with setting up the receiver connection, the app sends requests to the UDP server periodically
     * until either a response is received or the timeout is reached. This property defines the delay between two
     * requests that are sent.
     */
    val sendRequestInterval: Duration
) {
    companion object {
        /** A default value for the [networkTimeout] property. */
        val DEFAULT_TIMEOUT = 5.seconds

        /** A default value for the [retryDelay] property. */
        val DEFAULT_RETRY_DELAY = 10.seconds

        /** A default value for the [sendRequestInterval] property. */
        val DEFAULT_SEND_REQUEST_INTERVAL = 100.milliseconds
    }
}
