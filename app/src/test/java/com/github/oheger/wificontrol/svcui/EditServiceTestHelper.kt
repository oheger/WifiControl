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
package com.github.oheger.wificontrol.svcui

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag

import com.github.oheger.wificontrol.domain.model.PersistentService
import com.github.oheger.wificontrol.domain.model.ServiceAddressMode
import com.github.oheger.wificontrol.domain.model.ServiceDefinition
import com.github.oheger.wificontrol.performSafeClick
import com.github.oheger.wificontrol.setText

import io.kotest.inspectors.forAll

import kotlin.time.Duration.Companion.seconds

/**
 * A list with tags representing the UI elements to display invalid input for the several properties.
 */
internal val errorTags = listOf(TAG_EDIT_NAME, TAG_EDIT_MULTICAST, TAG_EDIT_PORT, TAG_EDIT_CODE)
    .map(::errorTag)

/**
 * Populate the edit fields of the form with the properties of the given [service]. The properties are selected based
 * on the service's address mode. If the [save] flag is *true*, also simulate a click on the save button.
 */
internal fun ComposeTestRule.enterServiceProperties(service: PersistentService, save: Boolean = true) {
    onNodeWithTag(TAG_EDIT_NAME).setText(service.serviceDefinition.name)
    when (service.serviceDefinition.addressMode) {
        ServiceAddressMode.WIFI_DISCOVERY -> {
            onNodeWithTag(TAG_EDIT_MULTICAST).setText(service.serviceDefinition.multicastAddress)
            onNodeWithTag(TAG_EDIT_PORT).setText(service.serviceDefinition.port.toString())
            onNodeWithTag(TAG_EDIT_CODE).setText(service.serviceDefinition.requestCode)
        }

        ServiceAddressMode.FIX_URL -> {
            onNodeWithTag(TAG_EDIT_SERVICE_URL).setText(service.serviceDefinition.serviceUrl)
        }
    }

    if (save) {
        saveForm()
    }
}

/**
 * Populate the edit field for the property identified by the given [tag] that supports a default value with the given
 * [value].
 */
internal fun ComposeTestRule.enterNonDefaultProperty(tag: String, value: String) {
    onNodeWithTag(useDefaultTag(tag)).performSafeClick()
    onNodeWithTag(tag).setText(value)
}

/**
 * Click the save button of the edit form, so that user input is evaluated.
 */
internal fun ComposeTestRule.saveForm() {
    onNodeWithTag(TAG_BTN_EDIT_SAVE).performSafeClick()
}

/**
 * Check that no UI element reporting an invalid input is currently displayed.
 */
internal fun ComposeTestRule.assertNoValidationErrors() {
    errorTags.forAll { tag ->
        onNodeWithTag(tag).assertDoesNotExist()
    }
}

/**
 * Check that all UI elements reporting invalid input are currently displayed.
 */
internal fun ComposeTestRule.assertAllValidationErrors() {
    errorTags.forAll { tag ->
        onNodeWithTag(tag).assertExists()
    }
}

/** A service definition that contains only invalid properties. This is used to test input validation. */
internal val errorServiceDefinition = ServiceDefinition(
    name = " ",
    ServiceAddressMode.WIFI_DISCOVERY,
    multicastAddress = "not an IP address",
    port = 70000,
    requestCode = " ",
    serviceUrl = ""
)

/** A service that contains only invalid properties. */
internal val errorService = PersistentService(
    serviceDefinition = errorServiceDefinition,
    lookupTimeout = 0.seconds,
    sendRequestInterval = 0.seconds
)
