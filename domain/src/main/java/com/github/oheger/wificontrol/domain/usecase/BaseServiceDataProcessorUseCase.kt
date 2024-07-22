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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * A base class for use cases that have to load and further process a [ServiceData] object. This base implementation
 * manages a [LoadServiceDataUseCase] object and invokes it to load the data. Derived classes need to implement a
 * function that expects the newly loaded [ServiceData] object and transforms it to their specific output type.
 */
abstract class BaseServiceDataProcessorUseCase<in I : BaseUseCase.Input, out O : BaseUseCase.Output>(
    config: UseCaseConfig,

    /** The use case for loading the [ServiceData] instance. */
    private val loadServiceDataUseCase: LoadServiceDataUseCase
) : BaseUseCase<I, O>(config) {
    override fun process(input: I): Flow<O> =
        loadServiceDataUseCase.process(LoadServiceDataUseCase.Input).map { output ->
            processServiceData(input, output.data)
        }

    /**
     * Process the given [data] object and produce the output of this use case using the given [input]. The base class
     * invokes this function when the central [ServiceData] instance has been loaded. A concrete implementation can do
     * whatever transformation.
     */
    protected abstract fun processServiceData(input: I, data: ServiceData): O
}
