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

import kotlinx.datetime.Instant

/**
 * The root interface of a hierarchy that defines the possible states when looking up a service in the network.
 *
 * The classes defined here are used by the UI for controlling services. Unless the URL of a service is already known,
 * the service needs to be discovered in the network. There is a corresponding state class that allows keeping track
 * on this process. Once the URL of the service is known, the state switches, and the URL can now be obtained from
 * the state to interact with the service.
 */
sealed interface LookupState

/**
 * A data class representing the state that the service needs to be discovered in the network. The class contains
 * some properties with additional information about the discovery process.
 */
data class LookupInProgress(
    /**
     * The start time of the discovery process. This can be used to find out how long the process is ongoing.
     */
    val startTime: Instant,

    /**
     * The number of attempts that were made to find the service. This corresponds to the UDP broadcast requests that
     * were sent over the network.
     */
    val attempts: Int
) : LookupState

/**
 * A data class representing the state that the URL of the service could be discovered. It can be obtained from the
 * state object.
 */
data class LookupSucceeded(
    /**
     * The URL under which the service can be accessed in the network.
     */
    val serviceUri: String
) : LookupState

/**
 * A special [LookupState] that is used to indicate that a service lookup operation has failed. When the timeout for
 * service discovery is reached without receiving an answer from the service, the lookup operation is terminated with
 * this state. Users can then manually trigger another attempt.
 */
data object LookupFailed : LookupState
