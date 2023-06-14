/*
 * Copyright 2023 Oliver Heger.
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

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4

import io.kotest.inspectors.forAll

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ControlUiTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `The NetworkStatusUnknown state is displayed correctly`() {
        checkNotificationsForState(NetworkStatusUnknown, textTag(TAG_WIFI_UNKNOWN), iconTag(TAG_WIFI_UNKNOWN))
    }

    @Test
    fun `The WiFiUnavailable state is displayed correctly`() {
        checkNotificationsForState(
            WiFiUnavailable,
            iconTag(TAG_WIFI_UNAVAILABLE),
            textTag(TAG_WIFI_UNAVAILABLE),
            TAG_WIFI_UNAVAILABLE_HINT
        )
    }

    @Test
    fun `The SearchingInWiFi state is displayed correctly`() {
        checkNotificationsForState(
            SearchingInWiFi,
            iconTag(TAG_WIFI_AVAILABLE),
            textTag(TAG_WIFI_AVAILABLE),
            iconTag(TAG_SEARCHING_FOR_SERVER),
            textTag(TAG_SEARCHING_FOR_SERVER),
            TAG_SEARCHING_HINT,
            TAG_SEARCHING_INDICATOR
        )
    }

    @Test
    fun `The ServerNotFound state is displayed correctly`() {
        checkNotificationsForState(
            ServerNotFound,
            iconTag(TAG_WIFI_AVAILABLE),
            textTag(TAG_WIFI_AVAILABLE),
            iconTag(TAG_SERVER_NOT_FOUND),
            textTag(TAG_SERVER_NOT_FOUND),
            TAG_SERVER_NOT_FOUND_HINT
        )
    }

    /**
     * Check whether for the given [state] the elements with the given [expectedTags] (and only those) are visible.
     */
    private fun checkNotificationsForState(state: ServerLookupState, vararg expectedTags: String) {
        val tagPrefixes = listOf(
            TAG_SEARCHING_FOR_SERVER,
            TAG_SERVER_NOT_FOUND,
            TAG_WIFI_AVAILABLE,
            TAG_WIFI_UNAVAILABLE,
            TAG_WIFI_UNKNOWN
        )
        val allTags = listOf(
            TAG_SEARCHING_HINT,
            TAG_SEARCHING_INDICATOR,
            TAG_WIFI_UNAVAILABLE_HINT,
            TAG_SERVER_NOT_FOUND_HINT
        ) + tagPrefixes.flatMap { prefix -> listOf(iconTag(prefix), textTag(prefix)) }
        val expectedTagsSet = expectedTags.toSet()

        val model = ControlViewModelImpl().apply { updateLookupState(state) }
        composeTestRule.activity.setContent { ControlUi(model) }

        allTags.filter { it in expectedTagsSet }.forAll { tag ->
            composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
        }
        allTags.filterNot { it in expectedTagsSet }.forAll { tag ->
            composeTestRule.onNodeWithTag(tag).assertDoesNotExist()
        }
    }
}
