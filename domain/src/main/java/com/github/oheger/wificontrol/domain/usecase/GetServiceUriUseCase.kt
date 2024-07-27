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

import com.github.oheger.wificontrol.domain.model.LookupService
import com.github.oheger.wificontrol.domain.model.LookupState
import com.github.oheger.wificontrol.domain.repo.ServiceUriRepository

import javax.inject.Inject

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * A use case for obtaining the URI of a service with a given name. The use case yields a [Flow] that allows keeping
 * track on the service discovery operation. When the URI of the service has been determined or the discovery
 * operation failed, the [Flow] ends with a final corresponding [LookupState] element.
 */
class GetServiceUriUseCase @Inject constructor(
    config: UseCaseConfig,

    /** The repository for looking up the service URI. */
    private val serviceUriRepository: ServiceUriRepository
) : BaseUseCase<GetServiceUriUseCase.Input, GetServiceUriUseCase.Output>(config) {
    override fun process(input: Input): Flow<Output> {
        return serviceUriRepository.lookupService(input.serviceName, input.lookupServiceProvider)
            .map { state -> Output(state) }
    }

    /**
     * The input type of this use case. Here the name of the desired service must be provided.
     */
    data class Input(
        /** The name of the service for which the URI should be retrieved. */
        val serviceName: String,

        /** The function to obtain the [LookupService] if a new lookup operation needs to be started. */
        val lookupServiceProvider: suspend () -> LookupService
    ) : BaseUseCase.Input

    /**
     * The output type of this use case. The use case returns a flow of [LookupState] objects that can be used to
     * keep track on the service discovery operation, and finally to retrieve the service URI.
     */
    data class Output(
        val lookupState: LookupState
    ) : BaseUseCase.Output
}