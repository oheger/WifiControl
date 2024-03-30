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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.github.oheger.wificontrol.domain.model.ServiceData
import com.github.oheger.wificontrol.domain.usecase.LoadServiceDataUseCase
import com.github.oheger.wificontrol.domain.usecase.StoreServiceDataUseCase
import com.github.oheger.wificontrol.svcui.ServicesUiState.Companion.combineState
import com.github.oheger.wificontrol.svcui.ServicesUiState.Companion.mapResultFlow

import javax.inject.Inject

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A data class describing the state of the services overview UI.
 */
data class ServicesOverviewState(
    /** The current [ServiceData] instance containing the managed services. */
    val serviceData: ServiceData,

    /**
     * A flag whether an update operation of the services caused an error. This means that the current state of the
     * application could not be saved successfully.
     */
    val updateError: Throwable? = null
)

/**
 * The view model for the UI showing the list of all services that can be controlled by this app.
 *
 * The list shows the service names and actions that can be applied on these services. It is also possible to edit
 * services from here or create new ones.
 */
class ServicesViewModel @Inject constructor(
    /** The use case to load the current [ServiceData] instance. */
    private val loadServicesUseCase: LoadServiceDataUseCase,

    /** The use case to store the [ServiceData] instance after it has been modified. */
    private val storeServicesUseCase: StoreServiceDataUseCase
) : ViewModel() {
    /** The flow to manage the current UI state. */
    private val mutableUiStateFlow = MutableStateFlow<ServicesUiState<ServicesOverviewState>>(ServicesUiStateLoading)

    /** A flow to keep track on errors that occur during saving of service data. */
    private val saveErrorFlow = MutableStateFlow<Throwable?>(null)

    val uiStateFlow = mutableUiStateFlow.asStateFlow().combineState(saveErrorFlow) { state, error ->
        state.copy(updateError = error)
    }

    fun loadServices() {
        viewModelScope.launch {
            loadServicesUseCase.execute(LoadServiceDataUseCase.Input)
                .mapResultFlow { result -> ServicesOverviewState(result.data) }
                .collect { state -> mutableUiStateFlow.value = state }
        }
    }

    fun moveServiceDown(serviceName: String) {
        modifyAndSaveData { it.moveDown(serviceName) }
    }

    fun moveServiceUp(serviceName: String) {
        modifyAndSaveData { it.moveUp(serviceName) }
    }

    fun removeService(serviceName: String) {
        modifyAndSaveData { it.removeService(serviceName) }
    }

    /**
     * Apply the given [modifyFunc] to the current [ServiceData] instance and then store the resulting object using
     * [StoreServiceDataUseCase].
     */
    private fun modifyAndSaveData(modifyFunc: (ServiceData) -> ServiceData) {
        viewModelScope.launch {
            val state = mutableUiStateFlow.value as ServicesUiStateLoaded
            storeServicesUseCase.execute(
                StoreServiceDataUseCase.Input(modifyFunc(state.data.serviceData))
            ).collect { result -> saveErrorFlow.value = result.exceptionOrNull() }
        }
    }
}
