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
package com.github.oheger.wificontrol.repository.di

import com.github.oheger.wificontrol.domain.repo.CurrentServiceRepository
import com.github.oheger.wificontrol.domain.repo.ServiceDataRepository
import com.github.oheger.wificontrol.domain.repo.ServiceUriRepository
import com.github.oheger.wificontrol.domain.repo.WiFiStateRepository
import com.github.oheger.wificontrol.repository.impl.CurrentServiceRepositoryImpl
import com.github.oheger.wificontrol.repository.impl.ServiceDataRepositoryImpl
import com.github.oheger.wificontrol.repository.impl.ServiceUriRepositoryImpl
import com.github.oheger.wificontrol.repository.impl.WiFiStateRepositoryImpl

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module that binds the implementations to repository interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun serviceDataRepository(serviceDataRepository: ServiceDataRepositoryImpl): ServiceDataRepository

    @Binds
    abstract fun currentServiceRepository(
        currentServiceRepositoryImpl: CurrentServiceRepositoryImpl
    ): CurrentServiceRepository

    @Binds
    abstract fun wiFiStateRepository(stateRepository: WiFiStateRepositoryImpl): WiFiStateRepository

    @Binds
    abstract fun serviceUriRepository(repositoryImpl: ServiceUriRepositoryImpl): ServiceUriRepository
}
