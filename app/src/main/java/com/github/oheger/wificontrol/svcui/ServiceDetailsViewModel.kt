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

import com.github.oheger.wificontrol.domain.model.PersistentService
import com.github.oheger.wificontrol.domain.model.ServiceData
import com.github.oheger.wificontrol.domain.usecase.LoadServiceUseCase
import com.github.oheger.wificontrol.domain.usecase.StoreServiceUseCase
import com.github.oheger.wificontrol.svcui.ServicesUiState.Companion.combineState
import com.github.oheger.wificontrol.svcui.ServicesUiState.Companion.mapResultFlow

import javax.inject.Inject

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A data class describing the state of the service details UI.
 */
data class ServiceDetailsState(
    /** The object storing the managed services. */
    val serviceData: ServiceData,

    /** The index of the service that is currently processed. */
    val serviceIndex: Int,

    /** The details of the current service. */
    val service: PersistentService,

    /** Flag whether the service should be edited. */
    val editMode: Boolean,

    /**
     * Stores an exception that occurred when saving a modified service. This causes an error message to be displayed
     * in the UI.
     */
    val saveError: Throwable? = null
)

/**
 * Abstract base class for the view model for the service details screen. This screen displays all the settings for a
 * specific service and also allows editing services or creating new ones.
 */
abstract class ServiceDetailsViewModel : ViewModel() {
    /** The flow providing the current state of the service details UI. */
    abstract val uiStateFlow: Flow<ServicesUiState<ServiceDetailsState>>

    /**
     * Load the current state of the service details UI for the service with the given [serviceIndex]. This will
     * trigger [uiStateFlow] when the data is available.
     */
    abstract fun loadService(serviceIndex: Int)

    /**
     * Switches the UI to edit mode. In this mode, the properties of the current service can be modified.
     */
    abstract fun editService()

    /**
     * Cancels the edit mode without saving the changes that might have been made on service properties.
     */
    abstract fun cancelEdit()

    /**
     * Make changes on the given [service] persisted. This function is used to save a service that has been edited.
     */
    abstract fun saveService(service: PersistentService)
}

/**
 * The default implementation of the view model for displaying and editing the details of a service.
 */
class ServiceDetailsViewModelImpl @Inject constructor(
    /** The use case for loading the service to be displayed. */
    private val loadServiceUseCase: LoadServiceUseCase,

    /** The use case for storing a service after it has been edited. */
    private val storeServiceUseCase: StoreServiceUseCase
) : ServiceDetailsViewModel() {
    /** The mutable flow to manage the current UI state. */
    private val mutableUiStateFlow = MutableStateFlow<ServicesUiState<ServiceDetailsState>>(ServicesUiStateLoading)

    /** A flow to keep track on errors that occur during saving of service data. */
    private val saveErrorFlow = MutableStateFlow<Throwable?>(null)

    /** A flow controlling whether edit mode is enabled or not. */
    private val editModeFlow = MutableStateFlow(false)

    override val uiStateFlow: Flow<ServicesUiState<ServiceDetailsState>> =
        mutableUiStateFlow.asStateFlow().combineState(editModeFlow) { state, editMode ->
            state.copy(editMode = editMode)
        }.combineState(saveErrorFlow) { state, error ->
            state.copy(saveError = error)
        }

    override fun loadService(serviceIndex: Int) {
        viewModelScope.launch {
            loadServiceUseCase.execute(LoadServiceUseCase.Input(serviceIndex)).mapResultFlow { result ->
                ServiceDetailsState(result.serviceData, serviceIndex, result.service, editMode = false)
            }.collect { state -> mutableUiStateFlow.value = state }
        }
    }

    override fun editService() {
        editModeFlow.value = true
    }

    override fun cancelEdit() {
        editModeFlow.value = false
    }

    override fun saveService(service: PersistentService) {
        (mutableUiStateFlow.value as? ServicesUiStateLoaded)?.let { state ->
            viewModelScope.launch {
                val storeInput = StoreServiceUseCase.Input(state.data.serviceData, service, state.data.serviceIndex)
                storeServiceUseCase.execute(storeInput).collect { result ->
                    val saveException = result.exceptionOrNull()
                    saveErrorFlow.value = saveException
                    editModeFlow.value = saveException != null
                }
            }
        }
    }
}
