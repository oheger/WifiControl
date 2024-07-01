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
package com.github.oheger.wificontrol.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.github.oheger.wificontrol.domain.model.CurrentService
import com.github.oheger.wificontrol.domain.usecase.StoreCurrentServiceUseCase

import kotlinx.coroutines.launch

/**
 * A base class for the view models used by this application. This base class provides some common functionality that
 * is required by each concrete view model implementation:
 * - It provides a function to load the UI state when the associated UI opens up.
 * - There is logic to ensure that this load function is only executed once.
 * - Each screen of the application has to update the name of the currently controlled service. This base class offers
 *   a function for this purpose and manages the use case required for this.
 */
abstract class BaseViewModel<in P : BaseViewModel.Parameters>(
    /** The use case for storing the currently controlled service in the preferences. */
    private val storeCurrentServiceUseCase: StoreCurrentServiceUseCase
) : ViewModel() {
    /**
     * A flag to control that the UI state is loaded only once. During recomposition, the [loadUiState]
     * function can be called multiple times. With this flag, only the first invocation triggers a load of the state.
     * Note: As all invocations happen on the main dispatcher, there is no need for synchronization.
     */
    private var stateLoaded = false

    /**
     * Initially load the UI state when the associated screen is opened based on the given [parameters]. This function
     * checks whether this is the first invocation. If so, it delegates to [performLoad]. Further calls are then
     * ignored.
     */
    fun loadUiState(parameters: P) {
        if (!stateLoaded) {
            stateLoaded = true

            val currentServiceToUpdate = performLoad(parameters)

            currentServiceToUpdate?.let(this::storeCurrentService)
        }
    }

    /**
     * Update the currently controlled service in the preferences based on the given [currentService].
     */
    protected fun storeCurrentService(currentService: CurrentService) {
        viewModelScope.launch {
            storeCurrentServiceUseCase.execute(StoreCurrentServiceUseCase.Input(currentService))
                .collect {}
        }
    }

    /**
     * Perform the actual initialization of this view model based on the given [parameters]. A concrete implementation
     * typically executes some use cases to obtain the data to be displayed by the UI. If a non-*null*
     * [CurrentService] is returned, this base class updates the name of the currently controlled service in the
     * preferences accordingly.
     */
    protected abstract fun performLoad(parameters: P): CurrentService?

    /**
     * A interface defining the parameters required by a specific instance. Such a parameters object is passed to the
     * [loadUiState] function.
     */
    interface Parameters
}
