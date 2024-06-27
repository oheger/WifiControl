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
package com.github.oheger.wificontrol.repository.ds

import kotlinx.coroutines.flow.Flow

/**
 * Definition of a data source interface for persisting the service that is currently controlled by the app. The
 * data source is used by the corresponding repository implementation. As the main use case of this data source is to
 * restore the last controlled service on startup of the app, the functions for loading and saving are a bit
 * asymmetric: The load function only initially returns a persisted service name; when invoked later again, it yields
 * an empty flow. The save function in contrast updates the service every time it is invoked.
 */
interface CurrentServiceDataSource {
    /**
     * Return a [Flow] with the name of the service to be controlled. This [Flow] only yields a result for the first
     * invocation of the function. This is used by the application logic to trigger the navigation to the control UI
     * if the service name is defined. The function can be called later again, but it then yields a [Flow] that does
     * not emit any value; hence, no navigation is performed.
     */
    fun loadCurrentServiceOnStartup(): Flow<String?>

    /**
     * Persists the given [serviceName] of the currently controlled service. A value of *null* means that there is
     * currently no service which is controlled; so no control UI can be restored.
     */
    suspend fun saveCurrentService(serviceName: String?)
}
