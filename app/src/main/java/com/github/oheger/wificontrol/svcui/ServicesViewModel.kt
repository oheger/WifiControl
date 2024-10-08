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

import androidx.lifecycle.viewModelScope

import com.github.oheger.wificontrol.domain.model.CurrentService
import com.github.oheger.wificontrol.domain.model.DefinedCurrentService
import com.github.oheger.wificontrol.domain.model.ServiceData
import com.github.oheger.wificontrol.domain.model.UndefinedCurrentService
import com.github.oheger.wificontrol.domain.usecase.LoadCurrentServiceUseCase
import com.github.oheger.wificontrol.domain.usecase.LoadServiceDataUseCase
import com.github.oheger.wificontrol.domain.usecase.StoreCurrentServiceUseCase
import com.github.oheger.wificontrol.domain.usecase.StoreServiceDataUseCase
import com.github.oheger.wificontrol.svcui.ServicesUiState.Companion.combineState
import com.github.oheger.wificontrol.svcui.ServicesUiState.Companion.mapResultFlow
import com.github.oheger.wificontrol.ui.BaseViewModel

import dagger.hilt.android.lifecycle.HiltViewModel

import javax.inject.Inject

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
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
@HiltViewModel
class ServicesViewModel @Inject constructor(
    /** The use case to load the current [ServiceData] instance. */
    private val loadServicesUseCase: LoadServiceDataUseCase,

    /** The use case to store the [ServiceData] instance after it has been modified. */
    private val storeServicesUseCase: StoreServiceDataUseCase,

    /** The use case for loading the current service to be controlled. */
    private val loadCurrentServiceUseCase: LoadCurrentServiceUseCase,

    /** The use case for storing the current service. */
    storeCurrentServiceUseCase: StoreCurrentServiceUseCase
) : BaseViewModel<ServicesViewModel.Parameters>(storeCurrentServiceUseCase) {
    /** The flow to manage the current UI state. */
    private val mutableUiStateFlow = MutableStateFlow<ServicesUiState<ServicesOverviewState>>(ServicesUiStateLoading)

    /** A flow to keep track on errors that occur during saving of service data. */
    private val saveErrorFlow = MutableStateFlow<Throwable?>(null)

    /** A flow to notify the UI when the last current service was loaded. */
    private val mutableCurrentServiceFlow = MutableSharedFlow<DefinedCurrentService>()

    /**
     * A flow that is monitored by the UI in order to receive the most recent UI state. The UI then updates itself
     * accordingly.
     */
    val uiStateFlow = mutableUiStateFlow.asStateFlow().combineState(saveErrorFlow) { state, error ->
        state.copy(updateError = error)
    }

    /**
     * A flow to notify the UI that the last current service has been loaded from the preferences. Then the control
     * UI for this service should be opened.
     */
    val currentServiceFlow = mutableCurrentServiceFlow.asSharedFlow()

    override fun performLoad(parameters: Parameters): CurrentService? {
        viewModelScope.launch {
            loadServicesUseCase.execute(LoadServiceDataUseCase.Input)
                .mapResultFlow { result -> ServicesOverviewState(result.data) }
                .collect { state -> mutableUiStateFlow.value = state }
        }

        viewModelScope.launch {
            val currentServiceFlow = MutableStateFlow<CurrentService>(UndefinedCurrentService)

            loadCurrentServiceUseCase.execute(LoadCurrentServiceUseCase.Input)
                .take(1)
                .mapNotNull { result -> result.getOrNull() }
                .map { it.currentService }
                .filterIsInstance<DefinedCurrentService>()
                .collect { currentService ->
                    mutableCurrentServiceFlow.emit(currentService)
                    currentServiceFlow.value = currentService
                }

            // Can only be done after the previous current service was loaded and processed.
            // An update of the current service must not be done if a defined service was loaded; then the
            // view logic navigates to the corresponding UI which also stores the current service.
            if (currentServiceFlow.value == UndefinedCurrentService) {
                storeCurrentService(UndefinedCurrentService)
            }
        }

        return null
    }

    /**
     * Move the service with the given [serviceName] one position down in the list of services.
     */
    fun moveServiceDown(serviceName: String) {
        modifyAndSaveData { it.moveDown(serviceName) }
    }

    /**
     * Move the service with the given [serviceName] one position up in the list of services.
     */
    fun moveServiceUp(serviceName: String) {
        modifyAndSaveData { it.moveUp(serviceName) }
    }

    /**
     * Remove the service with the given [serviceName] from the list of services.
     */
    fun removeService(serviceName: String) {
        modifyAndSaveData { it.removeService(serviceName) }
    }

    /**
     * Apply the given [modifyFunc] to the current [ServiceData] instance and then store the resulting object using
     * [StoreServiceDataUseCase].
     */
    private fun modifyAndSaveData(modifyFunc: (ServiceData) -> ServiceData) {
        mutableUiStateFlow.value.process { data ->
            viewModelScope.launch {
                storeServicesUseCase.execute(
                    StoreServiceDataUseCase.Input(modifyFunc(data.serviceData))
                ).collect { result -> saveErrorFlow.value = result.exceptionOrNull() }
            }
        }
    }

    /**
     * The type of parameters expected by this view model. The services overview screen does not require any
     * parameters.
     */
    data object Parameters : BaseViewModel.Parameters
}
