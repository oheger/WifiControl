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
package com.github.oheger.wificontrol.repository.impl

import com.github.oheger.wificontrol.domain.model.LookupInProgress
import com.github.oheger.wificontrol.domain.model.LookupService
import com.github.oheger.wificontrol.domain.model.LookupState
import com.github.oheger.wificontrol.domain.repo.ServiceUriRepository
import com.github.oheger.wificontrol.repository.ds.ServiceDiscoveryDataSource

import javax.inject.Inject

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformWhile

/**
 * An implementation of the [ServiceUriRepository] interface that uses a [ServiceDiscoveryDataSource] to determine
 * the URI of a service in the network.
 *
 * The heavy lifting is done by the data source. This implementation just delegates and terminates the flow with the
 * lookup state when the outcome of the discovery operation is known.
 */
class ServiceUriRepositoryImpl @Inject constructor(
    /** The data source that implements service discovery. */
    private val discoveryDataSource: ServiceDiscoveryDataSource
) : ServiceUriRepository {
    override fun lookupService(
        serviceName: String,
        lookupServiceProvider: suspend () -> LookupService
    ): Flow<LookupState> =
        discoveryDataSource.discoverService(serviceName, lookupServiceProvider).transformWhile { state ->
            emit(state)
            state is LookupInProgress
        }

    override fun clearService(serviceName: String) {
        discoveryDataSource.refreshService(serviceName)
    }
}
