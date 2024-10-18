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
import androidx.test.platform.app.InstrumentationRegistry

import com.github.oheger.wificontrol.Navigation
import com.github.oheger.wificontrol.R
import com.github.oheger.wificontrol.domain.model.DefinedCurrentService
import com.github.oheger.wificontrol.domain.model.LookupConfig
import com.github.oheger.wificontrol.domain.model.LookupFailed
import com.github.oheger.wificontrol.domain.model.LookupInProgress
import com.github.oheger.wificontrol.domain.model.LookupService
import com.github.oheger.wificontrol.domain.model.LookupState
import com.github.oheger.wificontrol.domain.model.LookupSucceeded
import com.github.oheger.wificontrol.domain.model.PersistentService
import com.github.oheger.wificontrol.domain.model.ServiceAddressMode
import com.github.oheger.wificontrol.domain.model.ServiceData
import com.github.oheger.wificontrol.domain.model.ServiceDefinition
import com.github.oheger.wificontrol.domain.model.WiFiState
import com.github.oheger.wificontrol.domain.usecase.ClearServiceUriUseCase
import com.github.oheger.wificontrol.domain.usecase.GetServiceUriUseCase
import com.github.oheger.wificontrol.domain.usecase.GetWiFiStateUseCase
import com.github.oheger.wificontrol.domain.usecase.LoadServiceByNameUseCase
import com.github.oheger.wificontrol.domain.usecase.StoreCurrentServiceUseCase

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify

import java.util.concurrent.atomic.AtomicInteger

import kotlin.time.Duration.Companion.seconds

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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

    /** The flow used to inject data about the current service into test cases. */
    private lateinit var loadServiceFlow: MutableSharedFlow<Result<LoadServiceByNameUseCase.Output>>

    /** The use case triggering a service discovery operation. */
    private lateinit var getServiceUriUseCase: GetServiceUriUseCase

    /** The use case for clearing the discovery state of a service. */
    private lateinit var clearServiceUriUseCase: ClearServiceUriUseCase

    /** A mock for a clock that is used to get deterministic time calculations. */
    private lateinit var testClock: Clock

    /** A counter to check whether the current service name has been saved. */
    private val storeCurrentServiceCounter = AtomicInteger()

    @Before
    fun setUp() {
        wiFiStateFlow = MutableSharedFlow()
        val getWiFiStateUseCase = mockk<GetWiFiStateUseCase> {
            every { execute(GetWiFiStateUseCase.Input) } returns wiFiStateFlow
        }

        lookupStateFlow = MutableSharedFlow()
        getServiceUriUseCase = mockk<GetServiceUriUseCase> {
            every { execute(any()) } returns lookupStateFlow
        }

        val storeCurrentServiceUseCase = mockk<StoreCurrentServiceUseCase> {
            every { execute(StoreCurrentServiceUseCase.Input(DefinedCurrentService(SERVICE_NAME))) } returns flow {
                storeCurrentServiceCounter.incrementAndGet()
            }
        }

        loadServiceFlow = MutableSharedFlow()
        val loadServiceUseCase = mockk<LoadServiceByNameUseCase> {
            every { execute(LoadServiceByNameUseCase.Input(SERVICE_NAME)) } returns loadServiceFlow
        }

        clearServiceUriUseCase = mockk()
        navController = mockk()
        testClock = mockk()
        initTime(Clock.System.now())

        val controlViewModel = ControlViewModel(
            getWiFiStateUseCase,
            getServiceUriUseCase,
            clearServiceUriUseCase,
            loadServiceUseCase,
            storeCurrentServiceUseCase,
            testClock
        )
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
     * Prepare the use case to clear the discovery result to expect an invocation and return the given [resultFlow].
     */
    private fun prepareClearUseCaseFlow(resultFlow: Flow<Result<ClearServiceUriUseCase.Output>>) {
        every { clearServiceUriUseCase.execute(ClearServiceUriUseCase.Input(SERVICE_NAME)) } returns resultFlow
    }

    /**
     * Prepare the use case to clear the discovery result to expect an invocation and return the given [result].
     */
    private fun prepareClearUseCase(
        result: Result<ClearServiceUriUseCase.Output> = Result.success(
            ClearServiceUriUseCase.Output
        )
    ) {
        prepareClearUseCaseFlow(flowOf(result))
    }

    /**
     * Sets the time to be returned by the test clock to the given [time]. This time will then be received by the view
     * model.
     */
    private fun initTime(time: Instant) {
        every { testClock.now() } returns time
    }

    /**
     * Prepare the mock for the [NavController] to expect a navigation request.
     */
    private fun expectNavigation() {
        every { navController.navigate(any<String>()) } just runs
    }

    /**
     * Emit the given [result] to the flow returned by the [LoadServiceByNameUseCase].
     */
    private suspend fun updateLoadServiceResult(result: Result<LoadServiceByNameUseCase.Output>) {
        loadServiceFlow.emit(result)
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
        expectNavigation()

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

        assertNotDisplayed(listOf(TAG_SERVICE_URI, TAG_FAILED_LOOKUP_HEADER))
    }

    /**
     * Check whether the UI elements are displayed to represent the Wi-Fi unavailable state - and only them.
     */
    private fun assertWiFiUnavailableUi() {
        composeTestRule.onNodeWithTag(textTag(TAG_WIFI_UNAVAILABLE)).assertIsDisplayed()
        assertNotDisplayed(dataTags)
    }

    @Test
    fun `An initial lookup state should be shown before a lookup state is received`() = runTest {
        updateWiFiState(WiFiState.WI_FI_AVAILABLE)

        composeTestRule.onNodeWithTag(TAG_LOOKUP_ATTEMPTS).assertTextContains("0")
        composeTestRule.onNodeWithTag(TAG_LOOKUP_TIME).assertTextContains("0")
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

    /**
     * Check whether the expected UI elements to display an error state are available - and only them.
     */
    private fun assertErrorState(detailsResId: Int, exception: Throwable) {
        val context = InstrumentationRegistry.getInstrumentation().context
        val expectedDetailsMessage = context.getText(detailsResId).toString()

        composeTestRule.onNodeWithTag(TAG_CTRL_ERROR_HEADER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_CTRL_ERROR_DETAILS)
            .assertTextEquals(expectedDetailsMessage)
        composeTestRule.onNodeWithTag(TAG_CTRL_ERROR_MESSAGE).assertIsDisplayed()
            .assertTextEquals(exception.toString())
        assertNotDisplayed(dataTags)
    }

    @Test
    fun `An error when querying the Wi-Fi state should be handled`() = runTest {
        val exception = IllegalArgumentException("Test Wi-Fi exception")
        updateWiFiState(WiFiState.WI_FI_AVAILABLE)

        updateWiFiStateResult(Result.failure(exception))

        assertErrorState(R.string.ctrl_error_details_wifi, exception)
    }

    @Test
    fun `An error when querying the lookup state should be handled`() = runTest {
        val exception = IllegalArgumentException("Test lookup exception")
        updateWiFiState(WiFiState.WI_FI_AVAILABLE)

        updateLookupStateResult(Result.failure(exception))

        assertErrorState(R.string.ctrl_error_details_lookup, exception)
    }

    @Test
    fun `A failed lookup operation should be displayed correctly`() = runTest {
        updateWiFiState(WiFiState.WI_FI_AVAILABLE)

        updateLookupState(LookupFailed)

        composeTestRule.onNodeWithTag(TAG_FAILED_LOOKUP_HEADER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(textTag(TAG_FAILED_LOOKUP_MESSAGE))
            .assertTextContains(SERVICE_NAME, substring = true)
        composeTestRule.onNodeWithTag(iconTag(TAG_FAILED_LOOKUP_MESSAGE)).assertIsDisplayed()
        assertNotDisplayed(listOf(TAG_SERVICE_URI))
    }

    @Test
    fun `A successfully discovered service should be displayed correctly`() = runTest {
        val serviceUri = "https://service.example.org/control/ui.html"
        updateWiFiState(WiFiState.WI_FI_AVAILABLE)

        updateLookupState(LookupSucceeded(serviceUri))

        composeTestRule.onNodeWithTag(TAG_SERVICE_URI).assertTextEquals(serviceUri)
        assertNotDisplayed(listOf(TAG_FAILED_LOOKUP_HEADER, TAG_LOOKUP_ATTEMPTS))
    }

    @Test
    fun `The correct LookupService should be passed to the discovery use case`() = runTest {
        val lookupService = LookupService(
            service = ServiceDefinition(
                SERVICE_NAME,
                ServiceAddressMode.WIFI_DISCOVERY,
                "231.0.0.1",
                4444,
                "code",
                ""
            ),
            lookupConfig = LookupConfig(30.seconds, 1.seconds)
        )
        val persistentService = PersistentService(
            serviceDefinition = lookupService.service,
            lookupTimeout = lookupService.lookupConfig.lookupTimeout,
            sendRequestInterval = lookupService.lookupConfig.sendRequestInterval
        )
        val serviceLoadResult = LoadServiceByNameUseCase.Output(ServiceData(listOf(persistentService)), lookupService)

        updateWiFiState(WiFiState.WI_FI_AVAILABLE)
        updateLoadServiceResult(Result.success(serviceLoadResult))

        val slotInput = slot<GetServiceUriUseCase.Input>()
        verify {
            getServiceUriUseCase.execute(capture(slotInput))
        }
        slotInput.captured.lookupServiceProvider() shouldBe lookupService
    }

    @Test
    fun `A button should be displayed to retry a failed discovery operation`() = runTest {
        updateWiFiState(WiFiState.WI_FI_AVAILABLE)
        updateLookupState(LookupFailed)
        prepareClearUseCase()

        composeTestRule.onNodeWithTag(TAG_BTN_RETRY_LOOKUP).performClick()

        val slotInputs = mutableListOf<GetServiceUriUseCase.Input>()
        verify(exactly = 2, timeout = 3000) {
            getServiceUriUseCase.execute(capture(slotInputs))
        }
        slotInputs.forAll {
            it.serviceName shouldBe SERVICE_NAME
        }
    }

    @Test
    fun `Collecting the lookup flow after a retry should stop when the Wi-Fi connection is lost`() = runTest {
        updateWiFiState(WiFiState.WI_FI_AVAILABLE)
        updateLookupState(LookupFailed)
        prepareClearUseCase()
        composeTestRule.onNodeWithTag(TAG_BTN_RETRY_LOOKUP).performClick()

        val lookUpStartTime = Clock.System.now() - 10.seconds
        updateLookupState(LookupInProgress(lookUpStartTime, 1))
        composeTestRule.onNodeWithTag(textTag(TAG_LOOKUP_MESSAGE)).assertIsDisplayed()

        updateWiFiState(WiFiState.WI_FI_UNAVAILABLE)
        updateLookupState(LookupInProgress(lookUpStartTime, 2))

        assertWiFiUnavailableUi()
    }

    @Test
    fun `An error of the clear URI use case should be ignored`() = runTest {
        updateWiFiState(WiFiState.WI_FI_AVAILABLE)
        updateLookupState(LookupFailed)
        prepareClearUseCase(Result.failure(IllegalArgumentException("Test exception")))

        composeTestRule.onNodeWithTag(TAG_BTN_RETRY_LOOKUP).performClick()

        verify(exactly = 2, timeout = 3000) {
            getServiceUriUseCase.execute(any())
        }
    }

    @Test
    fun `A failed lookup is retried only if there was no state change in the meantime`() = runTest {
        val clearUseCaseFlow = MutableSharedFlow<Result<ClearServiceUriUseCase.Output>>()
        updateWiFiState(WiFiState.WI_FI_AVAILABLE)
        updateLookupState(LookupFailed)
        prepareClearUseCaseFlow(clearUseCaseFlow)

        composeTestRule.onNodeWithTag(TAG_BTN_RETRY_LOOKUP).performClick()
        updateLookupState(LookupSucceeded("http://service.example.org/success"))
        clearUseCaseFlow.emit(Result.success(ClearServiceUriUseCase.Output))

        composeTestRule.onNodeWithTag(TAG_SERVICE_URI).assertIsDisplayed()
        verify(exactly = 1) {
            getServiceUriUseCase.execute(any())
        }
    }

    @Test
    fun `The name of the current service should be saved`() = runTest {
        eventually(3.seconds) {
            storeCurrentServiceCounter.get() shouldBe 1
        }
    }

    @Test
    fun `A button to navigate forward is displayed if there is a next service`() = runTest {
        expectNavigation()

        composeTestRule.onNodeWithTag(TAG_BTN_NAV_NEXT).assertDoesNotExist()
        updateWiFiState(WiFiState.WI_FI_AVAILABLE)
        composeTestRule.onNodeWithTag(TAG_BTN_NAV_NEXT).assertDoesNotExist()

        val nextService = createService("nextService")
        val serviceData = ServiceData(listOf(currentService, nextService))
        updateLoadServiceResult(Result.success(LoadServiceByNameUseCase.Output(serviceData, serviceData[0])))

        composeTestRule.onNodeWithTag(TAG_BTN_NAV_PREVIOUS).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_BTN_NAV_NEXT).performClick()

        verify {
            navController.navigate(
                Navigation.ControlServiceRoute.forArguments(
                    Navigation.ControlServiceArgs(
                        nextService.serviceDefinition.name
                    )
                )
            )
        }
    }

    @Test
    fun `A button to navigate backward is displayed if there is a previous service`() = runTest {
        expectNavigation()

        composeTestRule.onNodeWithTag(TAG_BTN_NAV_PREVIOUS).assertDoesNotExist()
        updateWiFiState(WiFiState.WI_FI_AVAILABLE)
        composeTestRule.onNodeWithTag(TAG_BTN_NAV_PREVIOUS).assertDoesNotExist()
        updateLookupState(LookupSucceeded("https://found-service.example.org/index.html"))
        composeTestRule.onNodeWithTag(TAG_BTN_NAV_PREVIOUS).assertDoesNotExist()

        val previousService = createService("prevService")
        val serviceData = ServiceData(listOf(previousService, currentService))
        updateLoadServiceResult(Result.success(LoadServiceByNameUseCase.Output(serviceData, serviceData[1])))

        composeTestRule.onNodeWithTag(TAG_BTN_NAV_NEXT).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_BTN_NAV_PREVIOUS).performClick()

        verify {
            navController.navigate(
                Navigation.ControlServiceRoute.forArguments(
                    Navigation.ControlServiceArgs(
                        previousService.serviceDefinition.name
                    )
                )
            )
        }
    }

    @Test
    fun `No navigation buttons should be displayed if the service could not be retrieved`() = runTest {
        updateWiFiState(WiFiState.WI_FI_AVAILABLE)
        updateLoadServiceResult(Result.failure(IllegalStateException("Test exception: Could not load service.")))

        composeTestRule.onNodeWithTag(TAG_BTN_NAV_PREVIOUS).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_BTN_NAV_NEXT).assertDoesNotExist()
    }

    @Test
    fun `No navigation buttons should be displayed if there is no Wi-Fi connection`() = runTest {
        val serviceData = ServiceData(listOf(createService("prev"), currentService, createService("next")))
        updateLoadServiceResult(Result.success(LoadServiceByNameUseCase.Output(serviceData, serviceData[1])))

        updateWiFiState(WiFiState.WI_FI_UNAVAILABLE)

        composeTestRule.onNodeWithTag(TAG_BTN_NAV_PREVIOUS).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_BTN_NAV_NEXT).assertDoesNotExist()
    }

    @Test
    fun `No navigation buttons should be displayed if there is an error in the control UI`() = runTest {
        updateWiFiState(WiFiState.WI_FI_AVAILABLE)

        updateLookupStateResult(Result.failure(IllegalArgumentException("Test lookup exception")))
        val serviceData = ServiceData(listOf(createService("prev"), currentService, createService("next")))
        updateLoadServiceResult(Result.success(LoadServiceByNameUseCase.Output(serviceData, serviceData[1])))

        composeTestRule.onNodeWithTag(TAG_BTN_NAV_PREVIOUS).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_BTN_NAV_NEXT).assertDoesNotExist()
    }

    /**
     * Check that none of the elements assigned to the given [tags] exists. This is used to verify the elements are
     * not visible that are not needed for a specific state of the UI.
     */
    private fun assertNotDisplayed(tags: Collection<String>) {
        tags.forAll { tag ->
            composeTestRule.onNodeWithTag(tag).assertDoesNotExist()
        }
    }
}

/** The name of the service to be controlled. */
private const val SERVICE_NAME = "serviceToControl"

/**
 * A collection with tags of elements that display data related to services. These elements should not be visible in
 * error state or when no Wi-Fi connection is available.
 */
private val dataTags = buildList {
    addAll(iconTextTags(listOf(TAG_WIFI_AVAILABLE, TAG_LOOKUP_MESSAGE, TAG_FAILED_LOOKUP_MESSAGE)))
    add(TAG_LOOKUP_ATTEMPTS)
    add(TAG_FAILED_LOOKUP_HEADER)
    add(TAG_SERVICE_URI)
}

/** The data of the test service. */
private val currentService = PersistentService(
    serviceDefinition = ServiceDefinition(
        SERVICE_NAME,
        ServiceAddressMode.WIFI_DISCOVERY,
        "231.0.0.7",
        8765,
        "code",
        ""
    ),
    lookupTimeout = null,
    sendRequestInterval = null
)

/**
 * Generate a collection with text and icon tags derived from the given [tags] collection.
 */
private fun iconTextTags(tags: Collection<String>): Collection<String> {
    val derivedTags = mutableSetOf<String>()
    tags.forEach { tag ->
        derivedTags += textTag(tag)
        derivedTags += iconTag(tag)
    }

    return derivedTags
}

/**
 * Create a new [PersistentService] as a copy of [currentService] with the given [name].
 */
private fun createService(name: String): PersistentService =
    currentService.copy(serviceDefinition = currentService.serviceDefinition.copy(name = name))
