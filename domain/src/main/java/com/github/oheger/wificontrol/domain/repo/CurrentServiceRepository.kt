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
package com.github.oheger.wificontrol.domain.repo

import com.github.oheger.wificontrol.domain.model.CurrentService

import kotlinx.coroutines.flow.Flow

/**
 * Definition of a repository interface for managing the service that is currently controlled.
 *
 * The current service is persisted, so that its control UI can be loaded and opened directly when the app is
 * restarted. This is rather useful if the user only deals with few services.
 */
interface CurrentServiceRepository {
    /**
     * Return a [Flow] with the persisted current service controlled by this application. Note that the primary use
     * case for this function is to restore the control UI of the current service when the app is started. This is not
     * used for navigation when the user switches to another service.
     */
    fun getCurrentService(): Flow<CurrentService>

    /**
     * Persist the given [currentService], so that its control UI can be restored when the app is closed and restarted.
     * This function is typically invoked every time the control UI is opened with the currently active service; and
     * also to clear this information when a different screen is shown.
     */
    fun setCurrentService(currentService: CurrentService): Flow<CurrentService>
}
