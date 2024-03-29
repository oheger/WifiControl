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

import com.github.oheger.wificontrol.domain.model.PersistentService
import com.github.oheger.wificontrol.domain.model.ServiceData

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * A specialized implementation of [ServicesViewModel] that is used for preview functions. The services to be
 * displayed are passed to the constructor. But it seems that the preview functionality cannot deal with dynamic flows
 * anyway. Therefore, this is just an easy-to-use implementation without any complex dependencies.
 */
class PreviewServicesViewModel(
    services: List<PersistentService>
) : ServicesViewModel() {
    private val internalServicesFlow = MutableStateFlow(services)

    override val uiStateFlow: Flow<ServicesUiState<ServicesOverviewState>>
        get() = internalServicesFlow.asStateFlow().map {
            ServicesUiStateLoaded(ServicesOverviewState(ServiceData(it, 0)))
        }

    override fun loadServices() {
    }

    override fun moveServiceDown(serviceName: String) {
    }

    override fun moveServiceUp(serviceName: String) {
    }

    override fun removeService(serviceName: String) {
    }
}
