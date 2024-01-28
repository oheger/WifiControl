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

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

import javax.inject.Inject

/**
 * An abstract base class for the view model used by the UI of this application.
 * 
 * Actually, the model is rather limited. It consists of the current lookup state, so that the UI can provide some
 * feedback about what is currently going on. When the server has been located in the network it presents its own UI
 * in a web view.
 */
abstract class ControlViewModel : ViewModel() {
    /**
     * The current [ServerLookupState].
     */
    abstract val lookupState: ServerLookupState

    /**
     * Set the current [ServerLookupState] to the given [state]. This function is invoked while performing the single
     * lookup steps. The UI can then keep track with the progress that is made.
     */
    abstract fun updateLookupState(state: ServerLookupState)
}

/**
 * A class serving as view model for the control UI.
 *
 * This class internally stores the current [ServerLookupState] and allows it to be updated. Note that all interaction
 * with an instance must be done on the main thread.
 */
class ControlViewModelImpl @Inject constructor() : ControlViewModel() {
    /** The field storing the current lookup state. */
    private val lookupStateField = mutableStateOf<ServerLookupState>(NetworkStatusUnknown)

    override val lookupState: ServerLookupState
        get() = lookupStateField.value

    override fun updateLookupState(state: ServerLookupState) {
        lookupStateField.value = state
    }
}
