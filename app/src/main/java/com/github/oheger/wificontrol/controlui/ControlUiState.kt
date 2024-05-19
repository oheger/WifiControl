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
 * A data class representing the state that the service to be controlled is currently looked up in the network. An
 * instance contains some further information about the ongoing discovery operation.
 */
data class ServiceDiscovery(
    /** Contains the number of attempts that have been made to reach the service. */
    val lookupAttempts: Int,

    /** The time duration how long the discovery operation is ongoing. */
    val lookupTime: Duration
) : ControlUiState

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
