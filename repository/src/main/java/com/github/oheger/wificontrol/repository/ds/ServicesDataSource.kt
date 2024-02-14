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

import com.github.oheger.wificontrol.domain.model.ServiceData

import kotlinx.coroutines.flow.Flow

/**
 * Definition of a data source interface for loading and storing the data about the services managed by this app.
 * This data source is used by the implementation of the repository for [ServiceData].
 */
interface ServicesDataSource {
    /**
     * Return a [Flow] with the [ServiceData] instance managed by this data source.
     */
    fun loadServiceData(): Flow<ServiceData>

    /**
     * Save the given [data] in this data source, overriding the former application state.
     */
    suspend fun saveServiceData(data: ServiceData)
}
