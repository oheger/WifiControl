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

import com.github.oheger.wificontrol.domain.usecase.LoadServiceByNameUseCase
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
 * Root of a hierarchy of classes that represent the state of loading the current service in the Control UI.
 * Dependent on the concrete subclass, more or less information is available in the UI.
 */
sealed interface ServiceLoadState {
    companion object {
        /** Constant for a [Pair] to be used if no navigation to other services is available. */
        val NAVIGATION_UNAVAILABLE: Pair<String?, String?> = Pair(null, null)
    }

    /**
     * Return a [Pair] with the names of the previous and next services for fast navigation in the UI. If one of the
     * names is defined, the UI displays a corresponding navigation action button. Concrete subclasses provide an
     * implementation that is suitable for the represented load state.
     */
    fun getNavigationServiceNames(): Pair<String?, String?>
}

/**
 * An object representing the state that information about the current service is not yet available. The load
 * operation is still ongoing.
 */
data object ServiceLoading : ServiceLoadState {
    override fun getNavigationServiceNames(): Pair<String?, String?> = ServiceLoadState.NAVIGATION_UNAVAILABLE
}

/**
 * A data class representing the state that the load operation for the current service has finished - either successful
 * or in failure state.
 */
data class ServiceLoadResult(
    /** The result of the load operation. */
    val loadResult: Result<LoadServiceByNameUseCase.Output>
) : ServiceLoadState {
    override fun getNavigationServiceNames(): Pair<String?, String?> =
        loadResult.map { it.serviceData.getPreviousAndNext(it.service.service.name) }
            .getOrElse { ServiceLoadState.NAVIGATION_UNAVAILABLE }
}

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
