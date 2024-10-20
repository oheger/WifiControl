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

import android.content.Context

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.NavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4

import com.github.oheger.wificontrol.Navigation
import com.github.oheger.wificontrol.R
import com.github.oheger.wificontrol.domain.model.PersistentService
import com.github.oheger.wificontrol.domain.model.ServiceAddressMode
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

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

import kotlinx.coroutines.flow.flowOf

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A UI test class that specifically tests the creation of a new service instance.
 */
@RunWith(AndroidJUnit4::class)
class CreateServiceUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /** The object storing the managed services. */
    private lateinit var serviceData: ServiceData

    /** Mock for the use case for storing the current service. */
    private lateinit var storeUseCase: StoreServiceUseCase

    /** Mock for the [NavController] to check whether navigation works as expected. */
    private lateinit var navController: NavController

    @Before
    fun setUp() {
        val testService = PersistentService(
            serviceDefinition = ServiceDefinition(
                "test",
                ServiceAddressMode.WIFI_DISCOVERY,
                "1.2.3.4",
                10000,
                "testCode",
                ""
            ),
            lookupTimeout = null,
            sendRequestInterval = null
        )
        val newService = PersistentService(
            serviceDefinition = ServiceDefinition("", ServiceAddressMode.WIFI_DISCOVERY, "", 0, "", ""),
            lookupTimeout = null,
            sendRequestInterval = null
        )
        serviceData = ServiceData(listOf(testService))

        val loadUseCase = mockk<LoadServiceUseCase> {
            every {
                execute(LoadServiceUseCase.Input(ServiceData.NEW_SERVICE_INDEX))
            } returns flowOf(Result.success(LoadServiceUseCase.Output(serviceData, newService)))
        }
        storeUseCase = mockk()
        navController = mockk()
        val viewModel = ServiceDetailsViewModel(loadUseCase, storeUseCase, mockk(relaxed = true))

        composeTestRule.setContent {
            ServiceDetailsScreen(
                viewModel = viewModel,
                serviceDetailsArgs = Navigation.ServiceDetailsArgs(ServiceData.NEW_SERVICE_INDEX),
                navController = navController
            )
        }
    }

    @Test
    fun `An empty form for service discovery is displayed initially`() {
        composeTestRule.onNodeWithTag(TAG_EDIT_URL_PROVIDED).assertIsOff()
        listOf(TAG_EDIT_NAME, TAG_EDIT_MULTICAST, TAG_EDIT_CODE).forAll { tag ->
            composeTestRule.onNodeWithTag(tag).assertTextEquals("")
        }
        composeTestRule.onNodeWithTag(TAG_EDIT_PORT).assertTextEquals("0")

        listOf(TAG_EDIT_LOOKUP_TIMEOUT, TAG_EDIT_REQUEST_INTERVAL).forAll { tag ->
            composeTestRule.onNodeWithTag(tag).assertDoesNotExist()
            composeTestRule.onNodeWithTag(useDefaultTag(tag)).assertIsOn()
        }

        composeTestRule.onNodeWithTag(TAG_EDIT_SERVICE_URL).assertDoesNotExist()
    }

    @Test
    fun `The address mode can be switched to a provided URL`() {
        composeTestRule.onNodeWithTag(TAG_EDIT_URL_PROVIDED).performSafeClick()
        composeTestRule.onNodeWithTag(TAG_EDIT_SERVICE_URL).assertTextEquals("")

        listOf(
            TAG_EDIT_MULTICAST,
            TAG_EDIT_PORT,
            TAG_EDIT_CODE,
            useDefaultTag(TAG_EDIT_LOOKUP_TIMEOUT),
            useDefaultTag(TAG_EDIT_REQUEST_INTERVAL)
        ).forAll {
            composeTestRule.onNodeWithTag(it).assertDoesNotExist()
        }
    }

    @Test
    fun `The title indicates that a new service is edited`() {
        val expectedTitle = ApplicationProvider.getApplicationContext<Context>().getString(R.string.svc_new_title)

        composeTestRule.onNodeWithTag(TAG_SVC_TITLE).assertTextEquals(expectedTitle)
    }

    @Test
    fun `A new service can be created and saved`() {
        every { storeUseCase.execute(any()) } returns flowOf(Result.success(StoreServiceUseCase.Output))
        expectNavigation()

        composeTestRule.enterServiceProperties(service)

        composeTestRule.assertNoValidationErrors()
        val expectedInput = StoreServiceUseCase.Input(
            data = serviceData,
            service = service,
            serviceIndex = ServiceData.NEW_SERVICE_INDEX
        )
        verify {
            storeUseCase.execute(expectedInput)
            navController.navigate(Navigation.ServicesRoute.route)
        }
    }

    @Test
    fun `A new service can be created with extended properties for discovery`() {
        every { storeUseCase.execute(any()) } returns flowOf(Result.success(StoreServiceUseCase.Output))
        expectNavigation()

        val testService = service.copy(lookupTimeout = 70.seconds, sendRequestInterval = 99.milliseconds)
        composeTestRule.enterServiceProperties(testService, save = false)
        composeTestRule.enterNonDefaultProperty(
            TAG_EDIT_LOOKUP_TIMEOUT,
            testService.lookupTimeout!!.inWholeSeconds.toString()
        )
        composeTestRule.enterNonDefaultProperty(
            TAG_EDIT_REQUEST_INTERVAL,
            testService.sendRequestInterval!!.inWholeMilliseconds.toString()
        )
        composeTestRule.saveForm()

        composeTestRule.assertNoValidationErrors()
        val expectedInput = StoreServiceUseCase.Input(
            data = serviceData,
            service = testService,
            serviceIndex = ServiceData.NEW_SERVICE_INDEX
        )
        verify {
            storeUseCase.execute(expectedInput)
            navController.navigate(Navigation.ServicesRoute.route)
        }
    }

    @Test
    fun `A new service can be created and saved with a provided URL`() {
        every { storeUseCase.execute(any()) } returns flowOf(Result.success(StoreServiceUseCase.Output))
        expectNavigation()

        composeTestRule.onNodeWithTag(TAG_EDIT_URL_PROVIDED).performSafeClick()
        composeTestRule.enterServiceProperties(serviceWithUrl)

        composeTestRule.assertNoValidationErrors()
        val expectedInput = StoreServiceUseCase.Input(
            data = serviceData,
            service = serviceWithUrl,
            serviceIndex = ServiceData.NEW_SERVICE_INDEX
        )
        verify {
            storeUseCase.execute(expectedInput)
            navController.navigate(Navigation.ServicesRoute.route)
        }
    }

    @Test
    fun `Editing a new service can be canceled`() {
        expectNavigation()

        composeTestRule.onNodeWithTag(TAG_EDIT_NAME).setText("My new service")
        composeTestRule.onNodeWithTag(TAG_BTN_EDIT_CANCEL).performSafeClick()

        verify {
            navController.navigate(Navigation.ServicesRoute.route)
        }
    }

    @Test
    fun `An error when saving the service is handled`() {
        val exception = IllegalStateException("Test exception: Could not save new service.")
        every { storeUseCase.execute(any()) } returns flowOf(Result.failure(exception))

        composeTestRule.enterServiceProperties(service)

        composeTestRule.onNodeWithTag(TAG_SAVE_ERROR).assertExists()
        composeTestRule.onNodeWithTag(TAG_SAVE_ERROR_MSG)
            .assertTextContains(exception.javaClass.simpleName, substring = true)
            .assertTextContains(exception.message!!, substring = true)
        composeTestRule.onNodeWithTag(TAG_EDIT_NAME).assertExists()
        composeTestRule.onNodeWithTag(TAG_SHOW_NAME).assertDoesNotExist()

        verify(exactly = 0) {
            navController.navigate(any<String>())
        }
    }

    @Test
    fun `Invalid input is detected and reported`() {
        composeTestRule.enterServiceProperties(errorService, save = false)

        composeTestRule.assertAllValidationErrors()
    }

    @Test
    fun `Invalid input for extended properties is detected and reported`() {
        composeTestRule.enterNonDefaultProperty(
            TAG_EDIT_LOOKUP_TIMEOUT,
            errorService.lookupTimeout!!.inWholeSeconds.toString()
        )
        composeTestRule.enterNonDefaultProperty(
            TAG_EDIT_REQUEST_INTERVAL,
            errorService.sendRequestInterval!!.inWholeMilliseconds.toString()
        )
        composeTestRule.saveForm()

        listOf(TAG_EDIT_LOOKUP_TIMEOUT, TAG_EDIT_REQUEST_INTERVAL).forAll { tag ->
            composeTestRule.onNodeWithTag(errorTag(tag)).assertExists()
        }
    }

    @Test
    fun `Invalid input for the provided URL properties is detected and reporter`() {
        val service = serviceWithUrl.copy(
            serviceDefinition = serviceWithUrl.serviceDefinition.copy(
                serviceUrl = "an invalid service URL?!"
            )
        )

        composeTestRule.onNodeWithTag(TAG_EDIT_URL_PROVIDED).performSafeClick()
        composeTestRule.enterServiceProperties(service, save = false)

        composeTestRule.onNodeWithTag(errorTag(TAG_EDIT_SERVICE_URL)).assertExists()
    }

    @Test
    fun `Clicking the save button performs a validation`() {
        composeTestRule.onNodeWithTag(TAG_EDIT_PORT).setText("70000")
        composeTestRule.saveForm()

        composeTestRule.assertAllValidationErrors()
    }

    /**
     * Prepare the mock for the [NavController] to expect an invocation of its `navigate()` function.
     */
    private fun expectNavigation() {
        every { navController.navigate(any<String>()) } just runs
    }
}

/** A new service that is created by test cases. */
private val service = PersistentService(
    serviceDefinition = ServiceDefinition(
        name = "TheNewService",
        addressMode = ServiceAddressMode.WIFI_DISCOVERY,
        multicastAddress = "231.1.2.3",
        port = 9007,
        requestCode = "testCode",
        serviceUrl = ""
    ),
    lookupTimeout = null,
    sendRequestInterval = null
)

/** A new service with a provided URL that is created by test cases. */
private val serviceWithUrl = PersistentService(
    serviceDefinition = ServiceDefinition(
        name = "TheNewServiceWithURL",
        addressMode = ServiceAddressMode.FIX_URL,
        multicastAddress = "",
        port = 0,
        requestCode = "",
        serviceUrl = "https://192.168.0.21/new-service/index.html"
    ),
    lookupTimeout = null,
    sendRequestInterval = null
)
