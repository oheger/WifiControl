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
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList

class CurrentServiceDataSourceImplTest : WordSpec({
    beforeTest {
        mockkStatic(DataStore<Preferences>::edit)
    }

    afterTest {
        unmockkAll()
        CurrentServiceDataSourceImpl.resetInvocationOnStartup()
    }

    "saveCurrentService" should {
        "store the name of the current service" {
            val preferences = mockk<MutablePreferences> {
                every { set(CurrentServiceDataSourceImpl.CURRENT_SERVICE_NAME_KEY, any()) } just runs
            }
            val dataStore = mockDataStoreForEdit(preferences)

            val dataSource = CurrentServiceDataSourceImpl(dataStore)
            dataSource.saveCurrentService(SERVICE_NAME)

            dataStore.executeEdit(preferences)
            verify {
                preferences[CurrentServiceDataSourceImpl.CURRENT_SERVICE_NAME_KEY] = SERVICE_NAME
            }
        }

        "remove the name of the current service if it is null" {
            val preferences = mockk<MutablePreferences> {
                every { remove(CurrentServiceDataSourceImpl.CURRENT_SERVICE_NAME_KEY) } returns ""
            }
            val dataStore = mockDataStoreForEdit(preferences)

            val dataSource = CurrentServiceDataSourceImpl(dataStore)
            dataSource.saveCurrentService(null)

            dataStore.executeEdit(preferences)
            verify {
                preferences.remove(CurrentServiceDataSourceImpl.CURRENT_SERVICE_NAME_KEY)
            }
        }
    }

    "loadCurrentServiceOnStartup" should {
        "return the flow retrieved from DataStore on first invocation" {
            val preferences = mockk<Preferences> {
                every { get(CurrentServiceDataSourceImpl.CURRENT_SERVICE_NAME_KEY) } returns SERVICE_NAME
            }
            val dataStore = mockk<DataStore<Preferences>> {
                every { data } returns flowOf(preferences)
            }

            val dataSource = CurrentServiceDataSourceImpl(dataStore)
            val serviceName = dataSource.loadCurrentServiceOnStartup().first()

            serviceName shouldBe SERVICE_NAME
        }

        "return an empty flow on succeeding invocations" {
            val preferences = mockk<Preferences> {
                every { get(CurrentServiceDataSourceImpl.CURRENT_SERVICE_NAME_KEY) } returns null
            }
            val dataStore = mockk<DataStore<Preferences>> {
                every { data } returns flowOf(preferences)
            }

            val dataSource1 = CurrentServiceDataSourceImpl(dataStore)
            dataSource1.loadCurrentServiceOnStartup().first().shouldBeNull()

            val dataSource2 = CurrentServiceDataSourceImpl(dataStore)
            val nextFlow = dataSource2.loadCurrentServiceOnStartup()
            val values = nextFlow.toList()

            values should beEmpty()
            verify(exactly = 1) {
                dataStore.data
            }
        }
    }
})

/** The name of the current service used by tests. */
private const val SERVICE_NAME = "theCurrentService"

/**
 * Create a mock for a [DataStore] that is prepared for a [DataStore.edit] operation.
 */
private fun mockDataStoreForEdit(preferences: MutablePreferences): DataStore<Preferences> =
    mockk {
        coEvery { edit(any()) } returns preferences
    }

/**
 * Execute an edit operation on this [DataStore] mock providing the given [preferences] to the transform function.
 */
private suspend fun DataStore<Preferences>.executeEdit(preferences: MutablePreferences) {
    val slot = slot<suspend (MutablePreferences) -> Unit>()
    coVerify { edit(capture(slot)) }
    slot.captured.invoke(preferences)
}
