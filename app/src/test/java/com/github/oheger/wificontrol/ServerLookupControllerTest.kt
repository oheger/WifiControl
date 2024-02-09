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
package com.github.oheger.wificontrol

import android.app.Activity

import com.github.oheger.wificontrol.domain.model.LookupService

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
class ServerLookupControllerTest : WordSpec() {
    /** The dispatcher used as Main coroutine context. */
    private lateinit var testDispatcher: ExecutorCoroutineDispatcher

    /** Mock for the activity. */
    private val activity = mockk<Activity>()

    init {
        beforeTest {
            testDispatcher = newSingleThreadContext("testDispatcher")
            Dispatchers.setMain(testDispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
            testDispatcher.close()
        }

        "create" should {
            "correctly initialize the new instance" {
                val model = mockk<ControlViewModel>()
                val config = mockk<LookupService>()

                val controller = ServerLookupController.create(model, activity, config)

                controller.activity shouldBe activity
                controller.serverFinder.lookupService shouldBe config
                controller.serverFinder.state shouldBe NetworkStatusUnknown
                controller.coroutineContext shouldBe Dispatchers.Main
            }
        }

        "startLookup" should {
            "update the model with new lookup states" {
                val model = mockk<ControlViewModel> {
                    every { updateLookupState(any()) } just runs
                }
                val queue = LinkedBlockingQueue<ServerLookupState>()
                val controller = ServerLookupController(
                    model,
                    activity,
                    createFinderMock(queue, NetworkStatusUnknown),
                    Dispatchers.Main
                )

                controller.startLookup()

                queue.offer(WiFiUnavailable)
                verify(timeout = 3000) {
                    model.updateLookupState(WiFiUnavailable)
                }

                queue.offer(SearchingInWiFi)
                verify(timeout = 3000) {
                    model.updateLookupState(SearchingInWiFi)
                }
            }

            "stop invoking the finder when the service was found" {
                val foundState = ServerFound("https://www.example.org/the/server.html")
                val model = mockk<ControlViewModel> {
                    every { updateLookupState(any()) } just runs
                }
                val states = LinkedBlockingQueue<ServerLookupState>()
                val finders = LinkedBlockingQueue<ServerFinder>()
                val initialFinder = createFinderMock(states, NetworkStatusUnknown, finders)
                val controller = ServerLookupController(model, activity, initialFinder, Dispatchers.Main)

                controller.startLookup()

                states.offer(foundState)
                verify(timeout = 3000) {
                    model.updateLookupState(foundState)
                }

                states.offer(ServerNotFound)

                finders.poll()
                finders.poll(250, TimeUnit.MILLISECONDS) should beNull()
            }
        }
    }

    /**
     * Create a mock [ServerFinder] that serves requests for the next state from the given [stateQueue] and yields the
     * given [currentState]. This simulates blocking behavior. Created finder mock objects are stored in the
     * given [finderQueue].
     */
    private fun createFinderMock(
        stateQueue: BlockingQueue<ServerLookupState>,
        currentState: ServerLookupState,
        finderQueue: BlockingQueue<ServerFinder> = LinkedBlockingQueue()
    ): ServerFinder =
        mockk {
            every { state } returns currentState
            coEvery { findServerStep(activity) } answers {
                val nextState = stateQueue.take()
                createFinderMock(stateQueue, nextState, finderQueue).also { finderQueue.offer(it) }
            }
        }
}
