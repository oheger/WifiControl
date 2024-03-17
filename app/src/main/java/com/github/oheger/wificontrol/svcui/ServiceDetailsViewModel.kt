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
import com.github.oheger.wificontrol.domain.usecase.LoadServiceUseCase

import javax.inject.Inject

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * A data class describing the state of the service details UI.
 */
data class ServiceDetailsState(
    /** The index of the service that is currently processed. */
    val serviceIndex: Int,

    /** The details of the current service. */
    val service: PersistentService
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
}

/**
 * The default implementation of the view model for displaying and editing the details of a service.
 */
class ServiceDetailsViewModelImpl @Inject constructor(
    /** The use case for loading the service to be displayed. */
    private val loadServiceUseCase: LoadServiceUseCase
) : ServiceDetailsViewModel() {
    /** The mutable flow to manage the current UI state. */
    private val mutableUiStateFlow = MutableStateFlow<ServicesUiState<ServiceDetailsState>>(ServicesUiStateLoading)

    override val uiStateFlow: Flow<ServicesUiState<ServiceDetailsState>> = mutableUiStateFlow.asStateFlow()

    override fun loadService(serviceIndex: Int) {
        viewModelScope.launch {
            loadServiceUseCase.execute(LoadServiceUseCase.Input(serviceIndex)).map { result ->
                result.map { ServicesUiStateLoaded(ServiceDetailsState(serviceIndex, it.service)) }
                    .getOrElse { ServicesUiStateError(it) }
            }.collect { state -> mutableUiStateFlow.value = state }
        }
    }
}
