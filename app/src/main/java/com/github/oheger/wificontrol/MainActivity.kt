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

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier

import com.github.oheger.wificontrol.ui.theme.WifiControlTheme

import dagger.hilt.android.AndroidEntryPoint

import javax.inject.Inject

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        /**
         * The configuration for searching the server in the network. This is currently hard-coded (which will change
         * later).
         */
        private val serverFinderConfig = ServerFinderConfig(
            multicastAddress = "231.10.0.0",
            port = 4321,
            requestCode = "playerServer?",
            networkTimeout = 5.seconds,
            retryDelay = 10.seconds,
            sendRequestInterval = 100.milliseconds
        )
    }

    /** The view model of the application. */
    @Inject internal lateinit var viewModel: ControlViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val controller = ServerLookupController.create(viewModel, this, serverFinderConfig)
        controller.startLookup()

        setContent {
            WifiControlTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    ControlUi(viewModel)
                }
            }
        }
    }
}
