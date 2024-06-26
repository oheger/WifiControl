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

import com.github.oheger.wificontrol.domain.model.CurrentService
import com.github.oheger.wificontrol.domain.repo.CurrentServiceRepository

import javax.inject.Inject

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * A use case for storing the [CurrentService] in the corresponding repository. The use case is invoked by every
 * screen to keep track on the currently controlled service, so that the control UI can be restored when starting the
 * app.
 */
class StoreCurrentServiceUseCase @Inject constructor(
    useCaseConfig: UseCaseConfig,

    /** The repository for storing the current service. */
    private val currentServiceRepository: CurrentServiceRepository
) : BaseUseCase<StoreCurrentServiceUseCase.Input, StoreCurrentServiceUseCase.Output>(useCaseConfig) {
    override fun process(input: Input): Flow<Output> =
        currentServiceRepository.setCurrentService(input.currentService).map { Output }

    /**
     * The type of the input of this use case. This is the service to be stored.
     */
    data class Input(
        /** The current service to be persisted. */
        val currentService: CurrentService
    ) : BaseUseCase.Input

    /**
     * The type of the output of this use case. Here no data is returned.
     */
    data object Output : BaseUseCase.Output
}
