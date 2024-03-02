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

import com.github.oheger.wificontrol.domain.model.ServiceData
import com.github.oheger.wificontrol.domain.repo.ServiceDataRepository

import javax.inject.Inject

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * A use case for storing an updated [ServiceData] instance. This is used to persist the application state after
 * there have been changes on the managed services.
 */
class StoreServiceDataUseCase @Inject constructor(
    /** The configuration of this use case. */
    config: UseCaseConfig,

    /** The repository to manage the current [ServiceData] object. */
    private val serviceDataRepository: ServiceDataRepository
) : BaseUseCase<StoreServiceDataUseCase.Input, StoreServiceDataUseCase.Output>(config) {
    override fun process(input: Input): Flow<Output> =
        serviceDataRepository.saveServiceData(input.data).map { Output }

    /**
     * The input type of this use case. Here the data to be stored must be provided.
     */
    data class Input(
        val data: ServiceData
    ) : BaseUseCase.Input

    /**
     * The output type of this use case. This use case does not return any data.
     */
    object Output : BaseUseCase.Output
}
