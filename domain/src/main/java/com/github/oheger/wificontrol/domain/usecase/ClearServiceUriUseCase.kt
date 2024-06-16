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

import com.github.oheger.wificontrol.domain.repo.ServiceUriRepository

import javax.inject.Inject

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A use case that clears the URI for a service in the [ServiceUriRepository]. The purpose of this use case is to
 * invalidate caches after properties of a service have been changed that might affect a discovery operation. It can
 * also be used to retry a discovery operation that failed in the past.
 */
class ClearServiceUriUseCase @Inject constructor(
    config: UseCaseConfig,

    /** The repository that manages the URIs of services to be controlled. */
    private val serviceUriRepository: ServiceUriRepository
) : BaseUseCase<ClearServiceUriUseCase.Input, ClearServiceUriUseCase.Output>(config) {
    override fun process(input: Input): Flow<Output> = flow {
        serviceUriRepository.clearService(input.serviceName)
        emit(Output)
    }

    /**
     * The input type of this use case. Here the service must be identified whose URI is to be removed from the
     * repository.
     */
    data class Input(
        /** The name of the service for which caches need to be invalidated. */
        val serviceName: String
    ) : BaseUseCase.Input

    /**
     * The output type of this use case. This use case does not return any result.
     */
    data object Output : BaseUseCase.Output
}
