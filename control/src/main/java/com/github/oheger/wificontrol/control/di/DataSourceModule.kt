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
package com.github.oheger.wificontrol.control.di

import android.content.Context

import com.github.oheger.wificontrol.control.source.ServiceDiscoveryDataSourceImpl
import com.github.oheger.wificontrol.control.source.WiFiStateDataSourceImpl
import com.github.oheger.wificontrol.repository.ds.ServiceDiscoveryDataSource
import com.github.oheger.wificontrol.repository.ds.WiFiStateDataSource

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

import javax.inject.Singleton

import kotlinx.coroutines.CoroutineScope

/**
 * Hilt module that provides the data source implementations hosted in this project.
 */
@Module
@InstallIn(SingletonComponent::class)
class DataSourceModule {
    @Provides
    @Singleton
    fun wiFiStateDataSource(@ApplicationContext context: Context): WiFiStateDataSource =
        WiFiStateDataSourceImpl.create(context)

    @Provides
    @Singleton
    fun serviceDiscoveryDataSource(scope: CoroutineScope): ServiceDiscoveryDataSource =
        ServiceDiscoveryDataSourceImpl(scope)
}
