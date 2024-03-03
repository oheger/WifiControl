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
import com.github.oheger.wificontrol.domain.usecase.LoadServiceDataUseCase
import com.github.oheger.wificontrol.domain.usecase.StoreServiceDataUseCase

import javax.inject.Inject

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Abstract base class for the view model for the services overview. This is used to support dummy implementations
 * for UI previews.
 */
abstract class ServicesViewModel : ViewModel() {
    /**
     * The flow providing the list of existing services.
     */
    abstract val servicesFlow: Flow<List<PersistentService>>

    /**
     * Load the current state of this app and initialize the view of services.
     */
    abstract fun loadServices()

    /**
     * Move the service with the given [serviceName] one position down in the list of services.
     */
    abstract fun moveServiceDown(serviceName: String)
}

/**
 * The view model for the UI showing the list of all services that can be controlled by this app.
 *
 * The list shows the service names and actions that can be applied on these services. It is also possible to edit
 * services from here or create new ones.
 */
class ServicesViewModelImpl @Inject constructor(
    /** The use case to load the current [ServiceData] instance. */
    private val loadServicesUseCase: LoadServiceDataUseCase,

    /** The use case to store the [ServiceData] instance after it has been modified. */
    private val storeServicesUseCase: StoreServiceDataUseCase
) : ServicesViewModel() {
    /** The flow to manage the current [ServiceData] state. */
    private val mutableServiceDataFlow = MutableStateFlow(ServiceData(emptyList(), 0))

    override val servicesFlow = mutableServiceDataFlow.asStateFlow().map { it.services }

    override fun loadServices() {
        viewModelScope.launch {
            loadServicesUseCase.execute(LoadServiceDataUseCase.Input)
                .map { result -> result.getOrThrow() }
                .collect { mutableServiceDataFlow.value = it.data }
        }
    }

    override fun moveServiceDown(serviceName: String) {
        viewModelScope.launch {
            storeServicesUseCase.execute(
                StoreServiceDataUseCase.Input(mutableServiceDataFlow.value.moveDown(serviceName))
            )
        }
    }
}
