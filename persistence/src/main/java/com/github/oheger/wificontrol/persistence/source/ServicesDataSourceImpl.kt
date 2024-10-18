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
package com.github.oheger.wificontrol.persistence.source

import androidx.datastore.core.DataStore

import com.github.oheger.wificontrol.domain.model.PersistentService
import com.github.oheger.wificontrol.domain.model.ServiceAddressMode
import com.github.oheger.wificontrol.domain.model.ServiceData
import com.github.oheger.wificontrol.domain.model.ServiceDefinition
import com.github.oheger.wificontrol.repository.ds.ServicesDataSource

import kotlin.time.Duration.Companion.milliseconds

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * An implementation of [ServicesDataSource] based on Proto DataStore. This implementation takes care of the conversion
 * between domain entities and the data structures used by DataStore to persist the state of the app in a file
 * managed by the DataStore library.
 */
class ServicesDataSourceImpl(
    private val dataStore: DataStore<PersistentServiceData>
) : ServicesDataSource {
    override fun loadServiceData(): Flow<ServiceData> =
        dataStore.data.map { persistentData ->
            ServiceData(
                services = persistentData.serviceDefinitionsList.map(::toDomainService)
            )
        }

    override suspend fun saveServiceData(data: ServiceData) {
        dataStore.updateData {
            val services = data.services.map(::toPersistentService)
            PersistentServiceData.newBuilder()
                .addAllServiceDefinitions(services)
                .build()
        }
    }
}

/**
 * Convert the given [serviceDefinition] to a [PersistentService] from the domain model.
 */
private fun toDomainService(serviceDefinition: PersistentServiceDefinition): PersistentService =
    with(serviceDefinition) {
        PersistentService(
            serviceDefinition = ServiceDefinition(
                name = name,
                addressMode = ServiceAddressMode.WIFI_DISCOVERY,
                multicastAddress = multicastAddress,
                port = port,
                requestCode = requestCode,
                serviceUrl = ""
            ),
            lookupTimeout = lookupTimeoutMs.takeIf { hasLookupTimeoutMs() }?.milliseconds,
            sendRequestInterval = sendRequestIntervalMs.takeIf { hasSendRequestIntervalMs() }?.milliseconds
        )
    }

/**
 * Convert the given [persistentService] to a [PersistentServiceDefinition] that can be saved in the data store.
 */
private fun toPersistentService(persistentService: PersistentService): PersistentServiceDefinition =
    with(persistentService) {
        val builder = PersistentServiceDefinition.newBuilder()
            .setName(serviceDefinition.name)
            .setMulticastAddress(serviceDefinition.multicastAddress)
            .setPort(serviceDefinition.port)
            .setRequestCode(serviceDefinition.requestCode)

        lookupTimeout?.let { builder.setLookupTimeoutMs(it.inWholeMilliseconds) }
        sendRequestInterval?.let { builder.setSendRequestIntervalMs(it.inWholeMilliseconds) }

        builder.build()
    }
