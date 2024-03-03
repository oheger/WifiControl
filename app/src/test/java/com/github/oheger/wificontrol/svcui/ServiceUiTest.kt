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
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServiceUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /** The flow used to inject data into the view model under test. */
    private lateinit var dataFlow: MutableStateFlow<Result<LoadServiceDataUseCase.Output>>

    /** A view model to serve the UI function. */
    private lateinit var servicesViewModel: ServicesViewModelImpl

    /** A mock for the use case to store the modified data instance. */
    private lateinit var storeDataUseCase: StoreServiceDataUseCase

    @Before
    fun setUp() {
        val initialData = ServiceData(emptyList(), 0)
        dataFlow = MutableStateFlow(Result.success(LoadServiceDataUseCase.Output(initialData)))
        val loadUseCase = mockk<LoadServiceDataUseCase> {
            every { execute(LoadServiceDataUseCase.Input) } returns dataFlow
        }

        storeDataUseCase = mockk()
        every { storeDataUseCase.execute(any()) } returns flowOf(Result.success(StoreServiceDataUseCase.Output))
        servicesViewModel = ServicesViewModelImpl(loadUseCase, storeDataUseCase)
        composeTestRule.setContent { ServicesScreen(viewModel = servicesViewModel) }
    }

    /**
     * Set the current value of the data flow to the given [data]. This data should then be picked up by the UI.
     */
    private fun initServiceData(data: ServiceData) {
        dataFlow.value = Result.success(LoadServiceDataUseCase.Output(data))
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
    fun `The list of services is displayed`() {
        val data = createServiceData(3)
        initServiceData(data)

        data.services.forAll { service ->
            composeTestRule.onNodeWithTag(serviceTag(service.serviceDefinition.name, TAG_SERVICE_NAME))
                .assertIsDisplayed()
                .assertTextEquals(service.serviceDefinition.name)
        }
    }

    @Test
    fun `An action to move down a service is available`() {
        val data = createServiceData(2).also(this::initServiceData)

        composeTestRule.onNodeWithTag(serviceTag(data.services.first().serviceDefinition.name, TAG_ACTION_DOWN))
            .performClick()
        val savedData = expectStoredData()

        savedData.currentIndex shouldBe data.currentIndex
        savedData.services shouldContainExactly listOf(data.services[1], data.services[0])
    }

    @Test
    fun `An action to move down a service is not displayed for the last element`() {
        val data = createServiceData(1).also(this::initServiceData)

        composeTestRule.onNodeWithTag(serviceTag(data.services.first().serviceDefinition.name, TAG_ACTION_DOWN))
            .assertDoesNotExist()
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
        networkTimeout = null,
        retryDelay = null,
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
