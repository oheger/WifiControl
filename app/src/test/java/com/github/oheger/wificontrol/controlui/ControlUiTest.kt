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
package com.github.oheger.wificontrol.controlui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavController
import androidx.test.ext.junit.runners.AndroidJUnit4

import com.github.oheger.wificontrol.Navigation
import com.github.oheger.wificontrol.domain.model.WiFiState
import com.github.oheger.wificontrol.domain.usecase.GetWiFiStateUseCase

import io.kotest.inspectors.forAll

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ControlUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /** A mock for the [NavController] for testing navigation actions. */
    private lateinit var navController: NavController

    /** The flow used to inject the Wi-Fi state into test cases. */
    private lateinit var wiFiStateFlow: MutableSharedFlow<Result<GetWiFiStateUseCase.Output>>

    @Before
    fun setUp() {
        wiFiStateFlow = MutableSharedFlow()
        val getWiFiStateUseCase = mockk<GetWiFiStateUseCase> {
            every { execute(GetWiFiStateUseCase.Input) } returns wiFiStateFlow
        }

        navController = mockk()
        val controlViewModel = ControlViewModel(getWiFiStateUseCase)
        composeTestRule.setContent {
            ControlScreen(
                viewModel = controlViewModel,
                controlArgs = Navigation.ControlServiceArgs(SERVICE_NAME),
                navController = navController
            )
        }
    }

    /**
     * Emit the given [result] to the flow returned by the [GetWiFiStateUseCase].
     */
    private suspend fun updateWiFiStateResult(result: Result<WiFiState>) {
        wiFiStateFlow.emit(result.map { GetWiFiStateUseCase.Output(it) })
    }

    /**
     * Convenience function to simulate an update of the Wi-Fi state to the given [state].
     */
    private suspend fun updateWiFiState(state: WiFiState) {
        updateWiFiStateResult(Result.success(state))
    }

    @Test
    fun `The Wi-Fi state unavailable should be assumed initially`() {
        composeTestRule.onNodeWithTag(textTag(TAG_WIFI_UNAVAILABLE)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(iconTag(TAG_WIFI_UNAVAILABLE)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(textTag(TAG_WIFI_AVAILABLE)).assertDoesNotExist()
    }

    @Test
    fun `The service name should be shown`() {
        composeTestRule.onNodeWithTag(TAG_SERVICE_NAME).assertTextEquals(SERVICE_NAME)
    }

    @Test
    fun `Navigation to the services overview UI should be possible`() {
        every { navController.navigate(any<String>()) } just runs

        composeTestRule.onNodeWithTag(TAG_BTN_NAV_OVERVIEW).performClick()

        verify {
            navController.navigate(Navigation.ServicesRoute.route)
        }
    }

    @Test
    fun `The looking up service state should be displayed when Wi-Fi is available`() = runTest {
        updateWiFiState(WiFiState.WI_FI_AVAILABLE)

        composeTestRule.onNodeWithTag(textTag(TAG_WIFI_UNAVAILABLE)).assertDoesNotExist()
        composeTestRule.onNodeWithTag(iconTag(TAG_WIFI_AVAILABLE)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(textTag(TAG_WIFI_AVAILABLE)).assertIsDisplayed()
    }

    @Test
    fun `The Wi-Fi unavailable UI should be shown if the WI-FI connection is lost`() = runTest {
        updateWiFiState(WiFiState.WI_FI_AVAILABLE)
        composeTestRule.onNodeWithTag(textTag(TAG_WIFI_UNAVAILABLE)).assertDoesNotExist()

        updateWiFiState(WiFiState.WI_FI_UNAVAILABLE)

        composeTestRule.onNodeWithTag(textTag(TAG_WIFI_UNAVAILABLE)).assertIsDisplayed()
        listOf(TAG_WIFI_AVAILABLE).forAll { tag ->
            composeTestRule.onNodeWithTag(textTag(tag)).assertDoesNotExist()
            composeTestRule.onNodeWithTag(iconTag(tag)).assertDoesNotExist()
        }
    }
}

/** The name of the service to be controlled. */
private const val SERVICE_NAME = "serviceToControl"
