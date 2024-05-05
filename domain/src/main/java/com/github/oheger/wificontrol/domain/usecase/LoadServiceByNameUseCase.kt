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

import com.github.oheger.wificontrol.domain.model.LookupService

import javax.inject.Inject

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * A use case for loading a specific [LookupService] by name. This is used in cases where services are
 * referenced by their names, especially in the control UI.
 */
class LoadServiceByNameUseCase @Inject constructor(
    config: UseCaseConfig,

    /** The use case for loading the whole data about services. */
    private val loadServiceDataUseCase: LoadServiceDataUseCase
) : BaseUseCase<LoadServiceByNameUseCase.Input, LoadServiceByNameUseCase.Output>(config) {

    override fun process(input: Input): Flow<Output> =
        loadServiceDataUseCase.process(LoadServiceDataUseCase.Input).map { loadResult ->
            Output(loadResult.data.getService(input.serviceName))
        }

    /**
     * The input type of this use case. Here the service to be loaded is specified.
     */
    data class Input(
        /** The name of the service to be loaded. */
        val serviceName: String
    ) : BaseUseCase.Input

    /**
     * The output type of this use case. Here the service that was loaded is returned.
     */
    data class Output(
        /** The service that was loaded by this use case. */
        val service: LookupService
    ) : BaseUseCase.Output
}