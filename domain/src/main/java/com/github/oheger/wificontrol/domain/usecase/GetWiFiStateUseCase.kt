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
package com.github.oheger.wificontrol.domain.usecase

import com.github.oheger.wificontrol.domain.model.WiFiState
import com.github.oheger.wificontrol.domain.repo.WiFiStateRepository

import javax.inject.Inject

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * A use case for obtaining a [Flow] that allows keeping track on the current state of the Wi-Fi connection. This is
 * used in the UI to disable all interaction with services when no network is available.
 */
class GetWiFiStateUseCase @Inject constructor(
    /** The configuration for this use case. */
    config: UseCaseConfig,

    /** The repository for obtaining the [WiFiState]. */
    private val stateRepository: WiFiStateRepository
) : BaseUseCase<GetWiFiStateUseCase.Input, GetWiFiStateUseCase.Output>(config) {
    override fun process(input: Input): Flow<Output> =
        stateRepository.getWiFiState().map(::Output)

    /**
     * The input type of this use case. Actually, no input is required.
     */
    object Input : BaseUseCase.Input

    data class Output(
        /** The current state of the Wi-Fi connection. */
        val wiFiState: WiFiState
    ) : BaseUseCase.Output
}
