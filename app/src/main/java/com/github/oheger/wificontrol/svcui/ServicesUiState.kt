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

import com.github.oheger.wificontrol.domain.model.ServiceData

/**
 * The top-level interface of a hierarchy of classes defining the state of the services UI. There are different
 * subclasses for the UI in loading state, when data has been loaded successfully, or if an error occurred.
 */
sealed interface ServicesUiState

/**
 * Data class representing the UI state that the data about the managed services has been loaded successfully.
 */
data class ServicesUiStateLoaded(
    /** The current [ServiceData] instance containing the managed services. */
    val serviceData: ServiceData,

    /**
     * A flag whether an update operation of the services caused an error. This means that the current state of the
     * application could not be saved successfully.
     */
    val updateError: Throwable? = null
) : ServicesUiState

/**
 * Data class representing the UI state that data about the managed services could not be loaded. In this case, only
 * an error message can be displayed in the UI.
 */
data class ServicesUiStateError(
    /** The error that occurred during loaded. */
    val error: Throwable
) : ServicesUiState

/**
 * An object representing the initial UI state that loading of data is in progress.
 */
data object ServicesUiStateLoading : ServicesUiState
