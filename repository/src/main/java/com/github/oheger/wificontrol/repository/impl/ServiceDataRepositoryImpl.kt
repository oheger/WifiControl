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

import com.github.oheger.wificontrol.domain.model.ServiceData
import com.github.oheger.wificontrol.domain.repo.ServiceDataRepository
import com.github.oheger.wificontrol.repository.ds.ServicesDataSource

import javax.inject.Inject

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

/**
 * An implementation of the [ServiceDataRepository] interface that uses a [ServicesDataSource] to load and save
 * [ServiceData] objects.
 */
class ServiceDataRepositoryImpl @Inject constructor(
    private val servicesSource: ServicesDataSource
) : ServiceDataRepository {
    override fun getServiceData(): Flow<ServiceData> =
        servicesSource.loadServiceData()

    override fun saveServiceData(data: ServiceData): Flow<ServiceData> = flow {
        servicesSource.saveServiceData(data)
        emit(Unit)
    }.flatMapLatest {
        servicesSource.loadServiceData()
    }
}
