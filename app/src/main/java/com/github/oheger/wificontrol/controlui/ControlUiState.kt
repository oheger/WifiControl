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
package com.github.oheger.wificontrol.controlui

import kotlin.time.Duration

/**
 * Root of a class hierarchy to represent the current state of a service discovery operation. If the UI state is not
 * in error and a Wi-Fi connection is available, it contains an instance of this type. Based on the concrete subclass,
 * the UI shows different information.
 */
sealed interface ControlDiscoveryState

/**
 * A data class representing the state that the service to be controlled is currently looked up in the network. An
 * instance contains some further information about the ongoing discovery operation.
 */
data class ServiceDiscovery(
    /** Contains the number of attempts that have been made to reach the service. */
    val lookupAttempts: Int,

    /** The time duration how long the discovery operation is ongoing. */
    val lookupTime: Duration
) : ControlDiscoveryState

/**
 * A state to represent the case that the service could not be discovered within the configured timeout.
 */
data object ServiceDiscoveryFailed : ControlDiscoveryState

/**
 * A data class representing the state that the service could be successfully discovered in the network. An instance
 * contains the required information to interact with the service.
 */
data class ServiceDiscoverySucceeded(
    /** The URI under which the UI of the service can be reached. */
    val uri: String
) : ControlDiscoveryState

/**
 * Root of a class hierarchy to represent the state of the UI that allows controlling services.
 *
 * Via the different classes in this hierarchy, the UI can determine whether the service to be controlled is already
 * fully known, and thus its control UI can be displayed. Otherwise, the UI has to show the current state of the
 * discovery process.
 */
sealed interface ControlUiState

/**
 * An object representing the state that no Wi-Fi connection is available. In this state, the app can only tell the
 * user that a Wi-Fi connection is required; no interactions with services is possible.
 */
data object WiFiUnavailable : ControlUiState

/**
 * A data class representing the state that an error occurred in the control UI. Actually, there could be different
 * errors, since multiple data sources are accessed. Therefore, this class holds some more detail information.
 */
data class ControlError(
    /** The resource ID of a message giving some background information in which context the error occurred. */
    val messageResId: Int,

    /** The exception that was actually thrown. */
    val cause: Throwable
) : ControlUiState

/**
 * A data class representing the state in which information about a service can be displayed. This means that a
 * discovery operation for this service could be started whose status is available and can be shown.
 */
data class ShowService(
    /** The status of the discovery operation for the current service. */
    val discoveryState: ControlDiscoveryState,

    /**
     * An optional name of a previous service in the list of services. This can be used to navigate to this service if
     * available.
     */
    val previousServiceName: String? = null,

    /**
     * An optional name of a next service in the list of services. This can be used to navigate to this service if
     * available. The names of the previous and next services allow a fast navigation forwards and backwards in the
     * list of services.
     */
    val nextServiceName: String? = null
) : ControlUiState
