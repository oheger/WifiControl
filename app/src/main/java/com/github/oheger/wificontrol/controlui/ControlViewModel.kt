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

import android.util.Log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.github.oheger.wificontrol.R
import com.github.oheger.wificontrol.domain.model.CurrentService
import com.github.oheger.wificontrol.domain.model.DefinedCurrentService
import com.github.oheger.wificontrol.domain.model.LookupFailed
import com.github.oheger.wificontrol.domain.model.LookupInProgress
import com.github.oheger.wificontrol.domain.model.LookupService
import com.github.oheger.wificontrol.domain.model.LookupState
import com.github.oheger.wificontrol.domain.model.LookupSucceeded
import com.github.oheger.wificontrol.domain.model.WiFiState
import com.github.oheger.wificontrol.domain.usecase.ClearServiceUriUseCase
import com.github.oheger.wificontrol.domain.usecase.GetServiceUriUseCase
import com.github.oheger.wificontrol.domain.usecase.GetWiFiStateUseCase
import com.github.oheger.wificontrol.domain.usecase.LoadServiceByNameUseCase
import com.github.oheger.wificontrol.domain.usecase.StoreCurrentServiceUseCase
import com.github.oheger.wificontrol.ui.BaseViewModel

import dagger.hilt.android.lifecycle.HiltViewModel

import javax.inject.Inject

import kotlin.time.Duration.Companion.seconds

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

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
    private val getWiFiStateUseCase: GetWiFiStateUseCase,

    /** The use case for obtaining the URI for the service to control. */
    private val getServiceUriUseCase: GetServiceUriUseCase,

    /** The use case to clear the URI of the service to control in order to retry the lookup operation. */
    private val clearServiceUriUseCase: ClearServiceUriUseCase,

    /** The use case for loading the current service by name. */
    private val loadServiceUseCase: LoadServiceByNameUseCase,

    /** The use case for storing the name of the currently controlled service. */
    storeCurrentServiceUseCase: StoreCurrentServiceUseCase,

    /** The clock to be used for time calculations. */
    private val clock: Clock = Clock.System
) : BaseViewModel<ControlViewModel.Parameters>(storeCurrentServiceUseCase) {
    companion object {
        private const val TAG = "ControlViewModel"

        /** Constant for an initial service discovery state. */
        private val initialDiscoveryState = ServiceDiscovery(0, 0.seconds)

        /** Constant for the UI state that is set at the beginning of a discovery operation. */
        private val initialShowServiceState = ShowService(initialDiscoveryState)
    }

    /** The internal flow for managing the UI state. */
    private val mutableUiStateFlow = MutableStateFlow<ControlUiState>(WiFiUnavailable)

    /** The internal flow to track the state of loading the current service. */
    private val mutableLoadStateFlow = MutableStateFlow<ServiceLoadState>(ServiceLoading)

    /** The internal flow to track the state of the current service discovery operation. */
    private val mutableDiscoveryStateFlow = MutableStateFlow<ControlDiscoveryState>(initialDiscoveryState)

    /** A flow that combines the current UI state with the state of the discovery operation. */
    private val discoveryUiStateFlow = mutableUiStateFlow
        .combine(mutableDiscoveryStateFlow) { uiState, discoveryState ->
            when (uiState) {
                is ShowService -> uiState.copy(discoveryState = discoveryState)
                else -> uiState
            }
        }

    /** The flow providing the current state of the Service Control UI for the current service. */
    val uiStateFlow: Flow<ControlUiState> = discoveryUiStateFlow
        .combine(mutableLoadStateFlow) { uiState, loadState ->
            when (uiState) {
                is ShowService -> {
                    val (prev, next) = loadState.getNavigationServiceNames()
                    uiState.copy(previousServiceName = prev, nextServiceName = next)
                }

                else -> uiState
            }
        }

    /**
     * Stores the job for tracking the service lookup state. This is needed to cancel the job again when the Wi-Fi
     * connection is lost.
     */
    private var lookupStateTrackingJob: Job? = null

    override fun performLoad(parameters: Parameters): CurrentService {
        viewModelScope.launch {
            getWiFiStateUseCase.execute(GetWiFiStateUseCase.Input)
                .collect { wiFiStateResult ->
                    wiFiStateResult.onSuccess { output ->
                        handleWiFiStateUpdate(parameters.serviceName, output.wiFiState)
                    }.onFailure { exception ->
                        mutableUiStateFlow.value = ControlError(R.string.ctrl_error_details_wifi, exception)
                    }
                }
        }

        viewModelScope.launch {
            loadServiceUseCase.execute(LoadServiceByNameUseCase.Input(parameters.serviceName))
                .collect { result -> mutableLoadStateFlow.value = ServiceLoadResult(result) }
        }

        return DefinedCurrentService(parameters.serviceName)
    }

    /**
     * Trigger another lookup operation for the service with the given [serviceName] when the previous one has failed.
     * This will lead to corresponding state updates.
     */
    fun retryFailedLookup(serviceName: String) {
        viewModelScope.launch {
            val clearResult = clearServiceUriUseCase.execute(ClearServiceUriUseCase.Input(serviceName)).first()
            if (clearResult.isFailure) {
                // No need for special error handling. If the service URI cannot be cleaned from the cache,
                // the retry will fail immediately, and the UI enters the same state again.
                Log.e(
                    TAG,
                    "Use case to clear the service URI failed with error: ${clearResult.exceptionOrNull()}"
                )
            }

            // Only start a new discovery operation if there was no state change in the meantime.
            val currentState = mutableUiStateFlow.value
            if (currentState is ShowService && mutableDiscoveryStateFlow.value == ServiceDiscoveryFailed) {
                updateLookupStateTrackingJob(trackLookupState(serviceName))
            }
        }
    }

    /**
     * Set the reference for the job that tracks a current lookup operation to the given [trackingJob]. Make sure that
     * a currently running job is canceled.
     */
    private fun updateLookupStateTrackingJob(trackingJob: Job?) {
        lookupStateTrackingJob?.cancel()
        lookupStateTrackingJob = trackingJob
    }

    /**
     * Deal with a change in the Wi-Fi connection state for the service with the given [serviceName]. Depending on
     * the new [wiFiState], either start tracking of the service lookup state or cancel the tracking job.
     */
    private fun CoroutineScope.handleWiFiStateUpdate(
        serviceName: String,
        wiFiState: WiFiState
    ) {
        Log.i(TAG, "Received Wi-Fi state $wiFiState.")
        when (wiFiState) {
            WiFiState.WI_FI_AVAILABLE -> {
                lookupStateTrackingJob = trackLookupState(serviceName)
            }

            WiFiState.WI_FI_UNAVAILABLE -> {
                mutableUiStateFlow.value = WiFiUnavailable
                updateLookupStateTrackingJob(null)
            }
        }
    }

    /**
     * Launch a [Job] that keeps track on changes of the lookup state for the service with the given [serviceName].
     * While the device is connected to Wi-Fi, this job updates the UI according to the state of the service
     * discovery operation.
     */
    private fun CoroutineScope.trackLookupState(serviceName: String): Job =
        launch {
            Log.i(TAG, "Starting a job to watch service discovery for '$serviceName'.")
            mutableDiscoveryStateFlow.value = initialDiscoveryState
            mutableUiStateFlow.value = initialShowServiceState

            getServiceUriUseCase.execute(GetServiceUriUseCase.Input(serviceName) { provideLookupService() })
                .collect { lookStateResult ->
                    lookStateResult.map { uiStateFromLookupState(it.lookupState) }
                        .onSuccess { discoveryState -> mutableDiscoveryStateFlow.value = discoveryState }
                        .onFailure { exception ->
                            mutableUiStateFlow.value = ControlError(R.string.ctrl_error_details_lookup, exception)
                        }
                }
        }

    /**
     * Return a [ControlDiscoveryState] to represent the given [lookupState]. Based on this, the control UI is
     * rendered.
     */
    private fun uiStateFromLookupState(lookupState: LookupState): ControlDiscoveryState =
        when (lookupState) {
            is LookupInProgress ->
                ServiceDiscovery(lookupState.attempts, clock.now() - lookupState.startTime)

            is LookupFailed ->
                ServiceDiscoveryFailed

            is LookupSucceeded ->
                ServiceDiscoverySucceeded(lookupState.serviceUri)
        }

    /**
     * A function that returns the [LookupService] for the current service in case this is needed for starting a new
     * discovery operation. This information can be obtained from the use case that loads the service.
     */
    private suspend fun provideLookupService(): LookupService =
        mutableLoadStateFlow.filterIsInstance<ServiceLoadResult>().first().loadResult
            .map(LoadServiceByNameUseCase.Output::service).getOrThrow()

    /**
     * A data class defining the parameters used by this view model. Here the name of the service to be controlled
     * is provided.
     */
    data class Parameters(
        /** The name of the current service. */
        val serviceName: String
    ) : BaseViewModel.Parameters
}
