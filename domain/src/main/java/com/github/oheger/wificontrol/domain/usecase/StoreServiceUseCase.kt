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
import com.github.oheger.wificontrol.domain.model.ServiceData.Companion.NEW_SERVICE_INDEX

import javax.inject.Inject

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * A use case for storing changes made on a [PersistentService] instance. The class updates the affected service in
 * the central [ServiceData] object and then delegates to [StoreServiceDataUseCase] to store this data. With this
 * use case newly created or modified services can be persisted.
 */
class StoreServiceUseCase @Inject constructor(
    config: UseCaseConfig,

    /** The use case for storing the updated [ServiceData] object. */
    private val storeDataUseCase: StoreServiceDataUseCase
) : BaseUseCase<StoreServiceUseCase.Input, StoreServiceUseCase.Output>(config) {
    override fun process(input: Input): Flow<Output> =
        updateFlow(input).flatMapConcat(storeDataUseCase::execute).map { Output }

    /**
     * Return a [Flow] that updates the current [ServiceData] in the given [Input] based on the specified parameters.
     * This flow is then piped through the use case to store the [ServiceData].
     */
    private fun updateFlow(input: Input): Flow<StoreServiceDataUseCase.Input> = flow {
        val newData = with(input) {
            if (serviceIndex == NEW_SERVICE_INDEX) {
                data.addService(service)
            } else {
                data.updateService(data.services[serviceIndex].serviceDefinition.name, service)
            }
        }

        emit(StoreServiceDataUseCase.Input(newData))
    }

    /**
     * The input type of this use case. It collects all information required to persist the changes on a single
     * service.
     */
    data class Input(
        /** The data object managing all services. */
        val data: ServiceData,

        /** The updated service that needs to be persisted. */
        val service: PersistentService,

        /**
         * The original index of the modified service in the [ServiceData] object. For newly created services, this
         * must be [NEW_SERVICE_INDEX].
         */
        val serviceIndex: Int
    ) : BaseUseCase.Input

    /**
     * The output type of this use case. This use case does not return any data.
     */
    object Output : BaseUseCase.Output
}
