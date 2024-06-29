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
package com.github.oheger.wificontrol.persistence.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

import com.github.oheger.wificontrol.persistence.source.CurrentServiceDataSourceImpl
import com.github.oheger.wificontrol.persistence.source.PersistentServiceData
import com.github.oheger.wificontrol.persistence.source.PersistentServiceDataSerializer
import com.github.oheger.wificontrol.persistence.source.ServicesDataSourceImpl

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

/** The name of the file to store data about services. */
private const val SERVICES_DATA_STORE_FILE = "services.pb"

/**
 * Extension property to obtain the data store for services.
 */
private val Context.serviceDataStore: DataStore<PersistentServiceData> by dataStore(
    fileName = SERVICES_DATA_STORE_FILE,
    serializer = PersistentServiceDataSerializer
)

/**
 * Extension property to obtain the data store for preferences.
 */
private val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "WiFiControlSettings")

/**
 * Hilt module that provides the dependencies for data source implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
class PersistenceModule {
    @Provides
    fun servicesDataSourceImpl(@ApplicationContext context: Context): ServicesDataSourceImpl =
        ServicesDataSourceImpl(context.serviceDataStore)

    @Provides
    fun currentServiceDataSourceImpl(@ApplicationContext context: Context): CurrentServiceDataSourceImpl =
        CurrentServiceDataSourceImpl(context.preferencesDataStore)
}
