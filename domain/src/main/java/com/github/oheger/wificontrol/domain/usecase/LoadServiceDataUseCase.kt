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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * A use case for loading the current [ServiceData] object from the corresponding repository.
 */
class LoadServiceDataUseCase @Inject constructor(
    /** The configuration of this use case. */
    private val config: UseCaseConfig,

    /** The repository to manage the current [ServiceData] object. */
    private val serviceDataRepository: ServiceDataRepository
) {
    /**
     * Execute this use case and return a [Flow] with the current [ServiceData] object wrapped in a [Result] to
     * handle failures.
     */
    fun execute(): Flow<Result<ServiceData>> =
        serviceDataRepository.getServiceData()
            .map { Result.success(it) }
            .flowOn(config.dispatcher)
            .catch {
                emit(Result.failure(it))
            }
}
