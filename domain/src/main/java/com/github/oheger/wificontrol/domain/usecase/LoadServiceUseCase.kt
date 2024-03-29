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

import com.github.oheger.wificontrol.domain.model.PersistentService
import com.github.oheger.wificontrol.domain.model.ServiceData

import javax.inject.Inject

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * A use case for loading a specific [PersistentService]. The services to be loaded is identified by its index in
 * the global [ServiceData] object. This is sufficient to identify the service uniquely, since it remains constant
 * while this specific service is displayed or edited.
 */
class LoadServiceUseCase @Inject constructor(
    config: UseCaseConfig,

    /** The use case for loading the whole data about services. */
    private val loadServiceDataUseCase: LoadServiceDataUseCase
) : BaseUseCase<LoadServiceUseCase.Input, LoadServiceUseCase.Output>(config) {
    override fun process(input: Input): Flow<Output> {
        return loadServiceDataUseCase.execute(LoadServiceDataUseCase.Input).map { dataResult ->
            dataResult.map { data ->
                val serviceData = data.data
                Output(serviceData,  serviceData.services[input.serviceIndex])
            }.getOrThrow()
        }
    }

    /**
     * The input type of this use case. Here the name of the service to be loaded is expected.
     */
    data class Input(
        /** The index of the service in the list of services to be loaded. */
        val serviceIndex: Int
    ) : BaseUseCase.Input

    /**
     * The output type of this use case. This contains the full [ServiceData] instance and the loaded service.
     */
    data class Output(
        /** The object containing all services. */
        val serviceData: ServiceData,

        /** The service that was loaded. */
        val service: PersistentService
    ) : BaseUseCase.Output
}
