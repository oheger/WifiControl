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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.github.oheger.wificontrol.controlui.ControlScreen
import com.github.oheger.wificontrol.svcui.ServiceDetailsScreen
import com.github.oheger.wificontrol.svcui.ServicesOverviewScreen
import com.github.oheger.wificontrol.ui.theme.WifiControlTheme

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WifiControlTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    val navController = rememberNavController()
                    App(navController)
                }
            }
        }
    }
}

/**
 * The main entry point into the UI of this App. This function defines the main screens and enables navigation
 * between them using the given [navController].
 */
@Composable
fun App(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Navigation.ServicesRoute.route) {
        composable(route = Navigation.ServicesRoute.route) {
            ServicesOverviewScreen(viewModel = hiltViewModel(), navController = navController)
        }

        composable(
            route = Navigation.ServiceDetailsRoute.route,
            arguments = Navigation.ServiceDetailsRoute.arguments
        ) {
            ServiceDetailsScreen(
                viewModel = hiltViewModel(),
                serviceDetailsArgs = Navigation.ServiceDetailsRoute.fromEntry(it),
                navController = navController
            )
        }

        composable(
            route = Navigation.ControlServiceRoute.route,
            arguments = Navigation.ControlServiceRoute.arguments
        ) {
            ControlScreen(
                viewModel = hiltViewModel(),
                controlArgs = Navigation.ControlServiceRoute.fromEntry(it),
                navController = navController
            )
        }
    }
}
