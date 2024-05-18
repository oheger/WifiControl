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
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavController
import androidx.test.ext.junit.runners.AndroidJUnit4

import com.github.oheger.wificontrol.Navigation
import com.github.oheger.wificontrol.domain.model.LookupInProgress
import com.github.oheger.wificontrol.domain.model.LookupState
import com.github.oheger.wificontrol.domain.model.WiFiState
import com.github.oheger.wificontrol.domain.usecase.GetServiceUriUseCase
import com.github.oheger.wificontrol.domain.usecase.GetWiFiStateUseCase

import io.kotest.inspectors.forAll

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

import kotlin.time.Duration.Companion.seconds

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

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

    /** The flow used to inject the current lookup state into test cases. */
    private lateinit var lookupStateFlow: MutableSharedFlow<Result<GetServiceUriUseCase.Output>>

    /** A mock for a clock that is used to get deterministic time calculations. */
    private lateinit var testClock: Clock

    @Before
    fun setUp() {
        wiFiStateFlow = MutableSharedFlow()
        val getWiFiStateUseCase = mockk<GetWiFiStateUseCase> {
            every { execute(GetWiFiStateUseCase.Input) } returns wiFiStateFlow
        }

        lookupStateFlow = MutableSharedFlow()
        val getServiceUriUseCase = mockk<GetServiceUriUseCase> {
            every { execute(GetServiceUriUseCase.Input(SERVICE_NAME)) } returns lookupStateFlow
        }

        navController = mockk()
        testClock = mockk()
        initTime(Clock.System.now())

        val controlViewModel = ControlViewModel(getWiFiStateUseCase, getServiceUriUseCase, testClock)
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

    /**
     * Emit the given [result] to the flow returned by the [GetServiceUriUseCase]. That way, a specific [LookupState]
     * can be injected.
     */
    private suspend fun updateLookupStateResult(result: Result<LookupState>) {
        lookupStateFlow.emit(result.map { GetServiceUriUseCase.Output(it) })
    }

    /**
     * Convenience function to simulate a successful update to the service lookup state to the given [state].
     */
    private suspend fun updateLookupState(state: LookupState) {
        updateLookupStateResult(Result.success(state))
    }

    /**
     * Sets the time to be returned by the test clock to the given [time]. This time will then be received by the view
     * model.
     */
    private fun initTime(time: Instant) {
        every { testClock.now() } returns time
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

        val lookupStartTime = Instant.parse("2024-05-17T19:05:21Z")
        initTime(lookupStartTime + 20.seconds)
        updateLookupState(LookupInProgress(lookupStartTime, 11))

        composeTestRule.onNodeWithTag(textTag(TAG_WIFI_UNAVAILABLE)).assertDoesNotExist()
        composeTestRule.onNodeWithTag(iconTag(TAG_WIFI_AVAILABLE)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(textTag(TAG_WIFI_AVAILABLE)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(textTag(TAG_LOOKUP_MESSAGE)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(iconTag(TAG_LOOKUP_MESSAGE)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_LOOKUP_ATTEMPTS).assertTextContains("11")
        composeTestRule.onNodeWithTag(TAG_LOOKUP_TIME).assertTextContains("20")
    }

    /**
     * Check whether the UI elements are displayed to represent the Wi-Fi unavailable state - and only them.
     */
    private fun assertWiFiUnavailableUi() {
        composeTestRule.onNodeWithTag(textTag(TAG_WIFI_UNAVAILABLE)).assertIsDisplayed()
        listOf(TAG_WIFI_AVAILABLE, TAG_LOOKUP_MESSAGE).forAll { tag ->
            composeTestRule.onNodeWithTag(textTag(tag)).assertDoesNotExist()
            composeTestRule.onNodeWithTag(iconTag(tag)).assertDoesNotExist()
        }
    }

    @Test
    fun `The Wi-Fi unavailable UI should be shown before a lookup state is received`() = runTest {
        updateWiFiState(WiFiState.WI_FI_AVAILABLE)

        assertWiFiUnavailableUi()
    }

    @Test
    fun `The Wi-Fi unavailable UI should be shown if the Wi-Fi connection is lost`() = runTest {
        updateWiFiState(WiFiState.WI_FI_AVAILABLE)
        updateLookupState(LookupInProgress(Clock.System.now() - 10.seconds, 1))
        composeTestRule.onNodeWithTag(textTag(TAG_WIFI_UNAVAILABLE)).assertDoesNotExist()

        updateWiFiState(WiFiState.WI_FI_UNAVAILABLE)

        assertWiFiUnavailableUi()
    }

    @Test
    fun `Collecting the lookup state flow should stop when the Wi-Fi connection is lost`() = runTest {
        updateWiFiState(WiFiState.WI_FI_AVAILABLE)
        val lookUpStartTime = Clock.System.now() - 10.seconds
        updateLookupState(LookupInProgress(lookUpStartTime, 1))

        updateWiFiState(WiFiState.WI_FI_UNAVAILABLE)
        updateLookupState(LookupInProgress(lookUpStartTime, 2))

        assertWiFiUnavailableUi()
    }
}

/** The name of the service to be controlled. */
private const val SERVICE_NAME = "serviceToControl"
