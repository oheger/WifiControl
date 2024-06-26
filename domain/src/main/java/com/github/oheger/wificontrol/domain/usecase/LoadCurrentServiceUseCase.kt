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
 * A use case for loading the [CurrentService] from the corresponding repository.
 */
class LoadCurrentServiceUseCase @Inject constructor(
    config: UseCaseConfig,

    /** The repository to manage the current service of this app. */
    private val currentServiceRepository: CurrentServiceRepository
) : BaseUseCase<LoadCurrentServiceUseCase.Input, LoadCurrentServiceUseCase.Output>(config) {
    override fun process(input: Input): Flow<Output> =
        currentServiceRepository.getCurrentService().map(::Output)

    /**
     * The type of the input of this use case. This use case does not require any input.
     */
    data object Input : BaseUseCase.Input

    /**
     * The type of the output of this use case. Here the current service is returned which may be defined or undefined.
     */
    data class Output(
        /** The currently controlled service. */
        val currentService: CurrentService
    ) : BaseUseCase.Output
}
