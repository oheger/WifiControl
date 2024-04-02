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
package com.github.oheger.wificontrol.svcui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * The top-level interface of a hierarchy of classes defining the state of the services UI. There are different
 * subclasses for the UI in loading state, when data has been loaded successfully, or if an error occurred.
 * In the success state, the data contained depends on the concrete UI screen. This is reflected by the type
 * parameter of this interface.
 */
sealed interface ServicesUiState<out T> {
    companion object {
        /**
         * Map this [Flow] of [Result] elements (typically the result [Flow] from a use case) to a [Flow] of
         * [ServicesUiState] elements with the help of the given [convert] function. The mapping of the [Result]
         * handles failure results automatically. For success results, it invokes the [convert] function and
         * produces a [ServicesUiStateLoaded] object with the result of this function.
         */
        fun <R, S> Flow<Result<R>>.mapResultFlow(convert: (R) -> S): Flow<ServicesUiState<S>> =
            map { result -> fromResult(result, convert) }

        /**
         * Combine a [Flow] of the current UI state with another [modifyFlow] using the given [combineFunc]. This
         * is typically used to extend the UI state loaded from the persistence layer by other dynamic data, for
         * instance based on user interaction.
         */
        fun <S, T> Flow<ServicesUiState<S>>.combineState(
            modifyFlow: Flow<T>,
            combineFunc: (S, T) -> S
        ): Flow<ServicesUiState<S>> =
            combine(modifyFlow) { state, value ->
                when (state) {
                    is ServicesUiStateLoaded -> state.copy(data = combineFunc(state.data, value))
                    else -> state
                }
            }

        /**
         * Convert the given [result] (typically from a use case) to a [ServicesUiState] object with the help of the
         * given [convert] function. This function handles failure results by itself. For success results, it
         * invokes the [convert] function and generates a [ServicesUiStateLoaded] object.
         */
        private fun <R, S> fromResult(result: Result<R>, convert: (R) -> S): ServicesUiState<S> =
            result.map { ServicesUiStateLoaded(convert(it)) }.getOrElse { ServicesUiStateError(it) }
    }

    /**
     * Apply the given processing function [f] to the state encapsulated in this instance if it is present. Return
     * the result of the processing function or *null* if the state has not yet been loaded.
     */
    fun <R> process(f: (T) -> R): R? =
        (this as? ServicesUiStateLoaded)?.let { f(it.data) }
}

/**
 * Data class representing the UI state that the data has been loaded successfully. It can now be accessed from a
 * field.
 */
data class ServicesUiStateLoaded<T>(
    val data: T
) : ServicesUiState<T>

/**
 * Data class representing the UI state that data about the managed services could not be loaded. In this case, only
 * an error message can be displayed in the UI.
 */
data class ServicesUiStateError(
    /** The error that occurred during loaded. */
    val error: Throwable
) : ServicesUiState<Nothing>

/**
 * An object representing the initial UI state that loading of data is in progress.
 */
data object ServicesUiStateLoading : ServicesUiState<Nothing>
