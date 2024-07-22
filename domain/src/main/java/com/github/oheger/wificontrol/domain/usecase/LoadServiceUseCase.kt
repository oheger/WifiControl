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

import com.github.oheger.wificontrol.domain.model.PersistentService
import com.github.oheger.wificontrol.domain.model.ServiceData
import com.github.oheger.wificontrol.domain.model.ServiceDefinition

import javax.inject.Inject

/**
 * A use case for loading a specific [PersistentService]. The service to be loaded is identified by its index in
 * the global [ServiceData] object. This is sufficient to identify the service uniquely, since it remains constant
 * while this specific service is displayed or edited. It is also possible to use the reserved index
 * [ServiceData.NEW_SERVICE_INDEX]; in this case, a new, empty service is created which can later be added to the
 * [ServiceData] object.
 */
class LoadServiceUseCase @Inject constructor(
    config: UseCaseConfig,
    loadServiceDataUseCase: LoadServiceDataUseCase
) : BaseServiceDataProcessorUseCase<LoadServiceUseCase.Input, LoadServiceUseCase.Output>(
    config,
    loadServiceDataUseCase
) {
    companion object {
        /** A service object with empty properties that is used when a new service is to be created. */
        private val newService = PersistentService(
            serviceDefinition = ServiceDefinition(
                name = "",
                multicastAddress = "",
                port = 0,
                requestCode = ""
            ),
            lookupTimeout = null,
            sendRequestInterval = null
        )
    }

    override fun processServiceData(input: Input, data: ServiceData): Output {
        val service = if (input.serviceIndex == ServiceData.NEW_SERVICE_INDEX) {
            newService
        } else {
            data.services[input.serviceIndex]
        }

        return Output(data, service)
    }

    /**
     * The input type of this use case. Here the name of the service to be loaded is expected.
     */
    data class Input(
        /** The index of the service in the list of services to be loaded. */
        val serviceIndex: Int
    ) : BaseUseCase.Input

    /**
     * The output type of this use case. This contains the full [ServiceData] instance and the loaded service.
     */
    data class Output(
        /** The object containing all services. */
        val serviceData: ServiceData,

        /** The service that was loaded. */
        val service: PersistentService
    ) : BaseUseCase.Output
}
