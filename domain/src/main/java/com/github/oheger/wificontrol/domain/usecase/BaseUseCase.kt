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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * An abstract base class for concrete use case implementations in the domain layer.
 *
 * The class defines a basic API based on input and output types; the result is always a [Flow] of a [Result] of the
 * given output type. It takes care of error handling and flow execution on the correct dispatcher.
 */
abstract class BaseUseCase<in I : BaseUseCase.Input, out O : BaseUseCase.Output>(
    /** The configuration for this use case. */
    private val config: UseCaseConfig
) {
    /**
     * Execute this use case with the given [input]. Return a [Flow] with the resulting data wrapped in a [Result].
     */
    fun execute(input: I): Flow<Result<O>> =
        process(input)
            .map { result -> Result.success(result) }
            .flowOn(config.dispatcher)
            .catch {
                emit(Result.failure(it))
            }

    /**
     * Return the [Flow] with the output of this use case. This is called by [execute] to obtain the actual result.
     * The returned [Flow] is then further mapped to handle errors and set other parameters.
     */
    protected abstract fun process(input: I): Flow<O>

    /**
     * A marker interface defining the input type for this use case. The [execute] function expects an object of this
     * type as argument.
     */
    interface Input

    /**
     * A marker interface defining the output type for this use case. This determines the type of the [Flow] returned
     * by the [execute] function.
     */
    interface Output
}
