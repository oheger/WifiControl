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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.oheger.wificontrol.Navigation

import com.github.oheger.wificontrol.domain.model.PersistentService
import com.github.oheger.wificontrol.domain.model.ServiceData
import com.github.oheger.wificontrol.domain.model.ServiceDefinition
import com.github.oheger.wificontrol.domain.usecase.LoadServiceUseCase
import com.github.oheger.wificontrol.domain.usecase.StoreServiceUseCase
import com.github.oheger.wificontrol.performSafeClick
import com.github.oheger.wificontrol.setText

import io.kotest.inspectors.forAll

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServiceDetailsUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /** The flow used to inject the test service into the view model under test. */
    private lateinit var dataFlow: MutableSharedFlow<Result<LoadServiceUseCase.Output>>

    /** The view model to serve the UI function. */
    private lateinit var detailsViewModel: ServiceDetailsViewModel

    /** Mock for the use case for storing the current service. */
    private lateinit var storeUseCase: StoreServiceUseCase

    /** Mock for the [NavController] to check the correct navigation. */
    private lateinit var navController: NavController

    @Before
    fun setUp() {
        dataFlow = MutableSharedFlow()
        val loadUseCaseInput = LoadServiceUseCase.Input(SERVICE_INDEX)
        val loadUseCase = mockk<LoadServiceUseCase> {
            every { execute(loadUseCaseInput) } returns dataFlow
        }

        storeUseCase = mockk()
        navController = mockk()
        detailsViewModel = ServiceDetailsViewModel(loadUseCase, storeUseCase)
        composeTestRule.setContent {
            ServiceDetailsScreen(
                viewModel = detailsViewModel,
                serviceDetailsArgs = Navigation.ServiceDetailsArgs(SERVICE_INDEX),
                navController
            )
        }
    }

    /**
     * Update the data flow to emit a result pointing to the given [service]. Return the generated output for the
     * load service use case.
     */
    private suspend fun initService(service: PersistentService): LoadServiceUseCase.Output {
        val serviceData = ServiceData(listOf(service), 0)
        val loadResult = LoadServiceUseCase.Output(serviceData, service)

        initLoadResult(Result.success(loadResult))
        return loadResult
    }

    /**
     * Set the current value of the data flow to the given [result]. This function allows setting arbitrary results,
     * including errors.
     */
    private suspend fun initLoadResult(result: Result<LoadServiceUseCase.Output>) {
        dataFlow.emit(result)
    }

    @Test
    fun `A loading indicator should be displayed while data is loaded`() {
        composeTestRule.onNodeWithTag(TAG_LOADING_INDICATOR).assertIsDisplayed()
    }

    @Test
    fun `An error view is shown if loading of the service failed`() = runTest {
        val exception = IllegalArgumentException("Some exception occurred :-(")
        initLoadResult(Result.failure(exception))

        composeTestRule.onNodeWithTag(TAG_ERROR_HEADER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_ERROR_MSG)
            .assertTextContains(exception.javaClass.simpleName, substring = true)
            .assertTextContains(exception.message!!, substring = true)
    }

    @Test
    fun `The details of the service should be displayed`() = runTest {
        initService(service)

        composeTestRule.onNodeWithTag(TAG_SVC_TITLE).assertTextEquals(service.serviceDefinition.name)
        composeTestRule.onNodeWithTag(TAG_SHOW_NAME).assertTextEquals(service.serviceDefinition.name)
        composeTestRule.onNodeWithTag(TAG_SHOW_MULTICAST).assertTextEquals(service.serviceDefinition.multicastAddress)
        composeTestRule.onNodeWithTag(TAG_SHOW_PORT).assertTextEquals(service.serviceDefinition.port.toString())
        composeTestRule.onNodeWithTag(TAG_SHOW_CODE).assertTextEquals(service.serviceDefinition.requestCode)

        listOf(TAG_EDIT_NAME, TAG_EDIT_MULTICAST, TAG_EDIT_PORT, TAG_EDIT_CODE).forAll {
            composeTestRule.onNodeWithTag(it).assertDoesNotExist()
        }
    }

    @Test
    fun `The edit mode of the service can be entered`() = runTest {
        initService(service)

        composeTestRule.onNodeWithTag(TAG_BTN_EDIT_SERVICE).performSafeClick()

        composeTestRule.onNodeWithTag(TAG_SVC_TITLE).assertTextEquals(service.serviceDefinition.name)
        composeTestRule.onNodeWithTag(TAG_EDIT_NAME).assertTextEquals(service.serviceDefinition.name)
        composeTestRule.onNodeWithTag(TAG_EDIT_MULTICAST).assertTextEquals(service.serviceDefinition.multicastAddress)
        composeTestRule.onNodeWithTag(TAG_EDIT_PORT).assertTextEquals(service.serviceDefinition.port.toString())
        composeTestRule.onNodeWithTag(TAG_EDIT_CODE).assertTextEquals(service.serviceDefinition.requestCode)

        listOf(TAG_SHOW_NAME, TAG_SHOW_MULTICAST, TAG_SHOW_PORT, TAG_SHOW_CODE).forAll {
            composeTestRule.onNodeWithTag(it).assertDoesNotExist()
        }
    }

    @Test
    fun `Editing of a service can be canceled`() = runTest {
        initService(service)

        composeTestRule.onNodeWithTag(TAG_BTN_EDIT_SERVICE).performClick()
        composeTestRule.onNodeWithTag(TAG_EDIT_CODE).setText("newRequestCode")

        composeTestRule.onNodeWithTag(TAG_BTN_EDIT_CANCEL).performSafeClick()

        composeTestRule.onNodeWithTag(TAG_SHOW_CODE).assertTextEquals(service.serviceDefinition.requestCode)
    }

    @Test
    fun `A service can be edited and saved`() = runTest {
        every { storeUseCase.execute(any()) } returns flowOf(Result.success(StoreServiceUseCase.Output))
        val loadOutput = initService(service)
        every { navController.navigate(any<String>()) } just runs

        val editedService = PersistentService(
            serviceDefinition = ServiceDefinition(
                name = "EditedTestService",
                multicastAddress = "231.0.0.9",
                port = 9875,
                requestCode = "AnybodyOutThere?!"
            ),
            networkTimeout = null,
            retryDelay = null,
            sendRequestInterval = null
        )

        composeTestRule.onNodeWithTag(TAG_BTN_EDIT_SERVICE).performSafeClick()
        composeTestRule.onNodeWithTag(TAG_EDIT_NAME).setText(editedService.serviceDefinition.name)
        composeTestRule.onNodeWithTag(TAG_EDIT_MULTICAST).setText(editedService.serviceDefinition.multicastAddress)
        composeTestRule.onNodeWithTag(TAG_EDIT_PORT).setText(editedService.serviceDefinition.port.toString())
        composeTestRule.onNodeWithTag(TAG_EDIT_CODE).setText(editedService.serviceDefinition.requestCode)
        composeTestRule.onNodeWithTag(TAG_BTN_EDIT_SAVE).performSafeClick()

        val expectedInput = StoreServiceUseCase.Input(
            data = loadOutput.serviceData,
            service = editedService,
            serviceIndex = SERVICE_INDEX
        )
        verify {
            storeUseCase.execute(expectedInput)
            navController.navigate(Navigation.ServicesRoute.route)
        }
    }

    @Test
    fun `For a failed save operation an error message should be shown`() = runTest {
        initService(service)
        val exception = IllegalStateException("Test exception: Could not save service.")
        every { storeUseCase.execute(any()) } returns flowOf(Result.failure(exception))

        composeTestRule.onNodeWithTag(TAG_BTN_EDIT_SERVICE).performSafeClick()
        composeTestRule.onNodeWithTag(TAG_BTN_EDIT_SAVE).performSafeClick()

        composeTestRule.onNodeWithTag(TAG_SAVE_ERROR).assertExists()
        composeTestRule.onNodeWithTag(TAG_SAVE_ERROR_MSG)
            .assertTextContains(exception.javaClass.simpleName, substring = true)
            .assertTextContains(exception.message!!, substring = true)
        composeTestRule.onNodeWithTag(TAG_EDIT_NAME).assertExists()
        composeTestRule.onNodeWithTag(TAG_SHOW_NAME).assertDoesNotExist()
    }

    @Test
    fun `There is a navigation button to go back to the overview screen`() = runTest {
        initService(service)
        every { navController.navigate(any<String>()) } just runs

        composeTestRule.onNodeWithTag(TAG_BTN_SVC_OVERVIEW).performClick()

        verify {
            navController.navigate(Navigation.ServicesRoute.route)
        }
    }
}

/** The index of the test service whose details should be shown. */
private const val SERVICE_INDEX = 11

/** A test service whose details can be displayed. */
private val service = PersistentService(
    serviceDefinition = ServiceDefinition(
        name = "TestService",
        multicastAddress = "231.0.0.8",
        port = 9876,
        requestCode = "AnybodyOutThere?"
    ),
    networkTimeout = null,
    retryDelay = null,
    sendRequestInterval = null
)
