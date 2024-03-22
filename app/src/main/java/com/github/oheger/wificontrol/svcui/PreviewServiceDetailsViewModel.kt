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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * A specialized implementation of [ServiceDetailsViewModel] that is used for preview functions.
 */
class PreviewServiceDetailsViewModel(
    service: PersistentService,
    private val editMode: Boolean = false
) : ServiceDetailsViewModel() {
    private val internalServiceFlow = MutableStateFlow(service)

    override val uiStateFlow: Flow<ServicesUiState<ServiceDetailsState>>
        get() = internalServiceFlow.asStateFlow().map {
            ServicesUiStateLoaded(ServiceDetailsState(0, it, editMode))
        }

    override fun loadService(serviceIndex: Int) {
    }

    override fun editService() {
    }
}
