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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.github.oheger.wificontrol.domain.model.WiFiState
import com.github.oheger.wificontrol.domain.usecase.GetWiFiStateUseCase

import dagger.hilt.android.lifecycle.HiltViewModel

import javax.inject.Inject

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

/**
 * The [ViewModel] for the UI to control a specific service.
 *
 * The model is responsible for obtaining the URL with the control UI of the current service. This may be a complex
 * operation requiring discovery in the network. The current state of the process is exposed as a [Flow], so that the
 * UI can react accordingly.
 */
@HiltViewModel
class ControlViewModel @Inject constructor(
    /** The use case for obtaining the Wi-Fi connection state. */
    private val getWiFiStateUseCase: GetWiFiStateUseCase
) : ViewModel() {
    /** The internal flow for managing the UI state. */
    private val mutableUiStateFlow = MutableStateFlow<ControlUiState>(WiFiUnavailable)

    /** The flow providing the current state of the Service Control UI for the current service. */
    val uiStateFlow: Flow<ControlUiState> = mutableUiStateFlow

    /**
     * Trigger the initialization of the [ControlUiState] for the current service. Changes in the state can then be
     * tracked via the [uiStateFlow] property.
     */
    fun initControlState() {
        viewModelScope.launch {
            getWiFiStateUseCase.execute(GetWiFiStateUseCase.Input)
                .mapNotNull { result -> result.getOrNull()?.wiFiState }
                .collect { wiFiState ->
                    val uiState = if (wiFiState == WiFiState.WI_FI_AVAILABLE) ServiceDiscovery else WiFiUnavailable
                    mutableUiStateFlow.value = uiState
                }
        }
    }
}