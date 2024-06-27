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

import com.github.oheger.wificontrol.domain.model.CurrentService
import com.github.oheger.wificontrol.domain.model.DefinedCurrentService
import com.github.oheger.wificontrol.domain.model.UndefinedCurrentService
import com.github.oheger.wificontrol.domain.repo.CurrentServiceRepository
import com.github.oheger.wificontrol.repository.ds.CurrentServiceDataSource

import javax.inject.Inject

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * An implementation of the [CurrentServiceRepository] interface that is based on a [CurrentServiceDataSource] for
 * persisting the name of the currently controlled service.
 */
class CurrentServiceRepositoryImpl @Inject constructor(
    /** The data source for loading and saving the current service. */
    private val currentServiceDataSource: CurrentServiceDataSource
) : CurrentServiceRepository {
    override fun getCurrentService(): Flow<CurrentService> =
        currentServiceDataSource.loadCurrentServiceOnStartup()
            .map { serviceName ->
                serviceName?.let(::DefinedCurrentService) ?: UndefinedCurrentService
            }

    override fun setCurrentService(currentService: CurrentService): Flow<CurrentService> = flow {
        val serviceName = when (currentService) {
            is DefinedCurrentService -> currentService.serviceName
            is UndefinedCurrentService -> null
        }

        currentServiceDataSource.saveCurrentService(serviceName)
        emit(Unit)
    }.flatMapLatest {
        getCurrentService()
    }
}
