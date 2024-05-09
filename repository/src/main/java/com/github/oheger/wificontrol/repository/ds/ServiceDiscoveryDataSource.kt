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
package com.github.oheger.wificontrol.repository.ds

import com.github.oheger.wificontrol.domain.model.LookupService
import com.github.oheger.wificontrol.domain.model.LookupState

import kotlinx.coroutines.flow.Flow

/**
 * Definition of a data source interface for handling service discovery.
 *
 * Via this interface, the URI of a service with a given name is looked up in the local network. The interface supports
 * caching; if the URI is already known, the discovery operation can directly be stopped with the corresponding result
 * state. Otherwise, the service properties required for the discovery operation are requested using a callback
 * function on demand; then discovery requests are sent in the network.
 */
interface ServiceDiscoveryDataSource {
    /**
     * Perform a discovery operation for the service with the given [serviceName] and return a [Flow] that allows
     * keeping track on the operation and getting the result. Use the given [lookupServiceProvider] to request a
     * [LookupService] if a new discovery operation needs to be started.
     */
    fun discoverService(serviceName: String, lookupServiceProvider: suspend () -> LookupService): Flow<LookupState>
}
