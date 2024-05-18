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
package com.github.oheger.wificontrol

import com.github.oheger.wificontrol.domain.usecase.UseCaseConfig

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.datetime.Clock

@Module
@InstallIn(SingletonComponent::class)
/**
 * A hilt module defining central singleton objects to be used by the DI framework.
 */
class AppModule {
    /**
     * Provide the central configuration for use cases. Here the dispatcher is defined on which use cases are
     * executed.
     */
    @Provides
    fun useCaseConfig(): UseCaseConfig =
        UseCaseConfig(Dispatchers.IO)

    /**
     * Provide the application-wide coroutine scope. In this scope, the coroutines are launched which need to run
     * independently on views.
     */
    @Provides
    fun applicationCoroutineScope(): CoroutineScope =
        CoroutineScope(SupervisorJob())

    /**
     * Provide the central [Clock] object for querying the current time.
     */
    @Provides
    fun systemClock(): Clock = Clock.System
}
