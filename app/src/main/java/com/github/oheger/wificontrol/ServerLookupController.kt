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

import kotlin.coroutines.CoroutineContext

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A class that handles the server lookup in background.
 *
 * An instance is initialized with a [ControlViewModel] and a [ServerFinder] instance. When started via the
 * [startLookup] function, it launches a background job that repeatedly calls the server finder and updates the view
 * model with the current [ServerLookupState], until the server is eventually found. Then the background job stops.
 */
class ServerLookupController(
    /** The view model to update. */
    val model: ControlViewModel,

    /** The current [Activity]. */
    val activity: Activity,

    /** The current [ServerFinder] instance. */
    val serverFinder: ServerFinder,

    /** The context to launch the background job. */
    override val coroutineContext: CoroutineContext
) : CoroutineScope {
    companion object {
        /**
         * Create a new [ServerLookupController] instance with a [ServerFinder] that uses the provided
         * [lookupService]. Update [model] when there are changes of the [ServerLookupState]. Pass the given
         * [activity] to the [ServerFinder].
         */
        fun create(
            model: ControlViewModel,
            activity: Activity,
            lookupService: LookupService
        ): ServerLookupController =
            ServerLookupController(model, activity, ServerFinder(lookupService), Dispatchers.Main)
    }

    /**
     * Start the server lookup operation in background.
     */
    fun startLookup() {
        launch { find(serverFinder) }
    }

    /**
     * Invoke the given [finder] and update the managed [model] with the next [ServerLookupState]. Trigger
     * further invocations until the server is finally found.
     */
    private tailrec suspend fun find(finder: ServerFinder) {
        val nextFinder = invokeFinder(finder)
        model.updateLookupState(nextFinder.state)

        if (nextFinder.state !is ServerFound) {
            find(nextFinder)
        }
    }

    /**
     * Invoke the given [finder] in the I/O context and return the result.
     */
    private suspend fun invokeFinder(finder: ServerFinder): ServerFinder = withContext(Dispatchers.IO) {
        finder.findServerStep(activity)
    }
}
