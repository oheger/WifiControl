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
import androidx.navigation.NavOptions
import androidx.test.ext.junit.runners.AndroidJUnit4

import com.github.oheger.wificontrol.domain.model.PersistentService
import com.github.oheger.wificontrol.domain.model.ServiceData
import com.github.oheger.wificontrol.domain.model.ServiceDefinition
import com.github.oheger.wificontrol.domain.usecase.LoadServiceDataUseCase
import com.github.oheger.wificontrol.domain.usecase.StoreServiceDataUseCase

import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServicesOverviewUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /** The flow used to inject data into the view model under test. */
    private lateinit var dataFlow: MutableSharedFlow<Result<LoadServiceDataUseCase.Output>>

    /** A view model to serve the UI function. */
    private lateinit var servicesViewModel: ServicesViewModel

    /** A mock for the use case to store the modified data instance. */
    private lateinit var storeDataUseCase: StoreServiceDataUseCase

    /** A mock for the [NavController]. */
    private lateinit var navController: NavController

    @Before
    fun setUp() {
        dataFlow = MutableSharedFlow()
        val loadUseCase = mockk<LoadServiceDataUseCase> {
            every { execute(LoadServiceDataUseCase.Input) } returns dataFlow
        }

        storeDataUseCase = mockk()
        every { storeDataUseCase.execute(any()) } returns flowOf(Result.success(StoreServiceDataUseCase.Output))
        servicesViewModel = ServicesViewModel(loadUseCase, storeDataUseCase)

        navController = mockk {
            every { navigate(any<String>(), any<NavOptions>()) } just runs
        }

        composeTestRule.setContent {
            ServicesOverviewScreen(viewModel = servicesViewModel, navController = navController)
        }
    }

    /**
     * Set the current value of the data flow to the given [data]. This data should then be picked up by the UI.
     */
    private suspend fun initServiceData(data: ServiceData): ServiceData {
        initLoadResult(Result.success(LoadServiceDataUseCase.Output(data)))
        return data
    }

    /**
     * Set the current value of the data flow to the given [result]. This function allows setting arbitrary results,
     * including errors.
     */
    private suspend fun initLoadResult(result: Result<LoadServiceDataUseCase.Output>) {
        dataFlow.emit(result)
    }

    /**
     * Expect a call to store the current [ServiceData] and return the instance that was passed to the use case.
     */
    private fun expectStoredData(): ServiceData {
        val slotData = slot<StoreServiceDataUseCase.Input>()
        verify {
            storeDataUseCase.execute(capture(slotData))
        }

        return slotData.captured.data
    }

    @Test
    fun `The list of services is displayed`() = runTest {
        val data = initServiceData(createServiceData(3))

        data.services.forAll { service ->
            composeTestRule.onNodeWithTag(serviceTag(service.serviceDefinition.name, TAG_SERVICE_NAME))
                .assertIsDisplayed()
                .assertTextEquals(service.serviceDefinition.name)
        }

        composeTestRule.onNodeWithTag(TAG_NO_SERVICES_MSG).assertDoesNotExist()
    }

    @Test
    fun `An action to move down a service is available`() = runTest {
        val data = initServiceData(createServiceData(2))

        composeTestRule.onNodeWithTag(serviceTag(data.services.first().serviceDefinition.name, TAG_ACTION_DOWN))
            .performClick()
        val savedData = expectStoredData()

        savedData.currentIndex shouldBe data.currentIndex
        savedData.services shouldContainExactly listOf(data.services[1], data.services[0])
    }

    @Test
    fun `An action to move down a service is not displayed for the last element`() = runTest {
        val data = initServiceData(createServiceData(1))

        composeTestRule.onNodeWithTag(serviceTag(data.services.first().serviceDefinition.name, TAG_ACTION_DOWN))
            .assertDoesNotExist()
    }

    @Test
    fun `An action to move up a service is available`() = runTest {
        val data = initServiceData(createServiceData(2))

        composeTestRule.onNodeWithTag(serviceTag(data.services[1].serviceDefinition.name, TAG_ACTION_UP))
            .performClick()
        val savedData = expectStoredData()

        savedData.currentIndex shouldBe data.currentIndex
        savedData.services shouldContainExactly listOf(data.services[1], data.services[0])
    }

    @Test
    fun `An action to move up a service is not displayed for the first element`() = runTest {
        val data = initServiceData(createServiceData(2))

        composeTestRule.onNodeWithTag(serviceTag(data.services.first().serviceDefinition.name, TAG_ACTION_UP))
            .assertDoesNotExist()
    }

    @Test
    fun `An action to delete a service is available`() = runTest {
        val data = initServiceData(createServiceData(3))

        composeTestRule.onNodeWithTag(serviceTag(data.services[0].serviceDefinition.name, TAG_ACTION_REMOVE))
            .performClick()
        val savedData = expectStoredData()

        savedData.currentIndex shouldBe data.currentIndex
        savedData.services shouldContainExactly listOf(data.services[1], data.services[2])
    }

    @Test
    fun `An action to go to service details is available`() = runTest {
        val serviceIndex = 3
        val data = initServiceData(createServiceData(5))

        composeTestRule
            .onNodeWithTag(serviceTag(data.services[serviceIndex].serviceDefinition.name, TAG_ACTION_DETAILS))
            .performClick()

        verify {
            navController.navigate("services/$serviceIndex")
        }
    }

    @Test
    fun `A message is displayed if no services exist`() = runTest {
        initServiceData(createServiceData(0))

        composeTestRule.onNodeWithTag(TAG_NO_SERVICES_MSG).assertIsDisplayed()
    }

    @Test
    fun `A loading indicator is displayed while data is loaded`() {
        composeTestRule.onNodeWithTag(TAG_LOADING_INDICATOR).assertIsDisplayed()
    }

    @Test
    fun `An error view is shown if loading of data failed`() = runTest {
        val exception = IllegalArgumentException("Some exception occurred :-(")
        initLoadResult(Result.failure(exception))

        composeTestRule.onNodeWithTag(TAG_ERROR_HEADER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_ERROR_MSG)
            .assertTextContains(exception.javaClass.simpleName, substring = true)
            .assertTextContains(exception.message!!, substring = true)
    }

    @Test
    fun `An error message is shown if service data cannot be saved`() = runTest {
        val exception = IllegalArgumentException("Some exception while saving :-O")
        every { storeDataUseCase.execute(any()) } returns flowOf(Result.failure(exception))
        val data = initServiceData(createServiceData(4))

        composeTestRule.onNodeWithTag(serviceTag(data.services[0].serviceDefinition.name, TAG_ACTION_REMOVE))
            .performClick()
        expectStoredData()

        composeTestRule.onNodeWithTag(TAG_SAVE_ERROR).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_SAVE_ERROR_MSG)
            .assertTextContains(exception.javaClass.simpleName, substring = true)
            .assertTextContains(exception.message!!, substring = true)
    }

    @Test
    fun `Navigation to a form to create a new service is possible`() = runTest {
        initServiceData(createServiceData(1))

        composeTestRule.onNodeWithTag(TAG_ACTION_CREATE).performClick()

        verify {
            navController.navigate("services/${ServiceData.NEW_SERVICE_INDEX}")
        }
    }

    @Test
    fun `Navigation to the control UI for a specific service is possible`() = runTest {
        val serviceData = initServiceData(createServiceData(3))
        val service = serviceData.services[1]

        composeTestRule.onNodeWithTag(serviceTag(service.serviceDefinition.name, TAG_SERVICE_NAME)).performClick()

        verify {
            navController.navigate("control/${service.serviceDefinition.name}")
        }
    }
}

/**
 * Generate the name of a test service based on the given [index].
 */
private fun serviceName(index: Int): String = "testService$index"

/**
 * Generate a [PersistentService] test instance based on the given [index].
 */
private fun createService(index: Int): PersistentService {
    val serviceDefinition = ServiceDefinition(
        name = serviceName(index),
        multicastAddress = "231.11.0.$index",
        port = 1000 + index,
        requestCode = "code_$index"
    )
    return PersistentService(
        serviceDefinition = serviceDefinition,
        lookupTimeout = null,
        sendRequestInterval = null
    )
}

/**
 * Generate a [ServiceData] instance with [serviceCount] test services.
 */
private fun createServiceData(serviceCount: Int): ServiceData {
    val services = (1..serviceCount).map(::createService)
    return ServiceData(services, 0)
}
