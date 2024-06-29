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

import android.util.Log

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey

import com.github.oheger.wificontrol.repository.ds.CurrentServiceDataSource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * An implementation of the [CurrentServiceDataSource] interface based on Preferences DataStore. This implementation
 * persists the currently controlled service by storing its name (or nothing if there is no current service) as a
 * string property in the preferences. The service name is only loaded once on the first invocation of the load
 * function.
 */
class CurrentServiceDataSourceImpl(
    /** The object for storing key value pairs. */
    private val dataStore: DataStore<Preferences>
) : CurrentServiceDataSource {
    companion object {
        /** Constant for the key that stores the name of the current service. */
        internal val CURRENT_SERVICE_NAME_KEY = stringPreferencesKey("currentServiceName")

        /** A tag for logging. */
        private const val TAG = "CurrentServiceDataSourceImpl"

        /**
         * An internal flow that records whether the current service has already been retrieved. This is used to
         * implement the functionality that [loadCurrentServiceOnStartup] only once queries the [DataStore] and
         * afterward returns an empty flow.
         */
        private val invocationOnStartup = MutableStateFlow(true)

        /**
         * Reset the [invocationOnStartup] flow. This is needed for testing only.
         */
        internal fun resetInvocationOnStartup() {
            invocationOnStartup.value = true
        }
    }

    override fun loadCurrentServiceOnStartup(): Flow<String?> = flow {
        if(invocationOnStartup.compareAndSet(expect = true, update = false)) {
            Log.i(TAG, "Loading the current service from preferences.")

            emitAll(
                dataStore.data.map { preferences ->
                    preferences[CURRENT_SERVICE_NAME_KEY]
                }
            )
        }
    }

    override suspend fun saveCurrentService(serviceName: String?) {
        Log.i(TAG, "Updating the current service to '$serviceName'.")

        dataStore.edit { preferences ->
            if(serviceName != null) {
                preferences[CURRENT_SERVICE_NAME_KEY] = serviceName
            } else {
                preferences.remove(CURRENT_SERVICE_NAME_KEY)
            }
        }
    }
}
