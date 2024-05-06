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
package com.github.oheger.wificontrol.domain.repo

import com.github.oheger.wificontrol.domain.model.LookupService
import com.github.oheger.wificontrol.domain.model.LookupState

import kotlinx.coroutines.flow.Flow

/**
 * Definition of a repository interface for looking up the URI of a specific service by its name.
 *
 * The intended workflow is as follows: The repository returns a [Flow] with the [LookupState] of the requested
 * service. This can be used to update the UI while service discovery over the network is in progress. Once the
 * service URI has been detected, the corresponding state is emitted, and the [Flow] terminates. If the underlying
 * data source implements caching, the next time the service is queried, its URI is available immediately.
 */
interface ServiceUriRepository {
    /**
     * Return a [Flow] with the current [LookupState] for the service with the given [serviceName]. If the URI of
     * this service is known, the corresponding state is directly emitted. Otherwise, a discovery operation needs to
     * be started. For this purpose, the [LookupService] instance for the affected service is required. It is obtained
     * by invoking the given [lookupServiceProvider] function. After emitting a state that indicates the result of the
     * operation, the flow terminates.
     */
    fun lookupService(serviceName: String, lookupServiceProvider: suspend () -> LookupService): Flow<LookupState>
}
