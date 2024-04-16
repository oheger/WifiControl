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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

import com.github.oheger.wificontrol.Navigation
import com.github.oheger.wificontrol.R
import com.github.oheger.wificontrol.TAG_WIFI_UNAVAILABLE_HINT
import com.github.oheger.wificontrol.ui.theme.WifiControlTheme

internal const val TAG_BTN_NAV_OVERVIEW = "ctrlBtnNavOverview"
internal const val TAG_SERVICE_NAME = "ctrlServiceName"
internal const val TAG_WIFI_AVAILABLE = "ctrlWiFiAvailable"
internal const val TAG_WIFI_UNAVAILABLE = "ctrlWiFiUnavailable"

/**
 * Generate the full tag for a text message related to the given base [tag].
 */
internal fun textTag(tag: String): String = "${tag}_text"

/**
 * Generate the full tag for an icon related to the given [tag]
 */
internal fun iconTag(tag: String): String = "${tag}_icon"

/**
 * The main entry point into the UI to control a specific service. The UI shows the different states when obtaining
 * the control UI of the service identified by the given [controlArgs]. Once the UI is available, it is opened in a
 * Web view. The given [viewModel] is responsible for updating the UI state accordingly. Use the given
 * [navController] to navigate to other application screens.
 */
@Composable
fun ControlScreen(
    viewModel: ControlViewModel,
    controlArgs: Navigation.ControlServiceArgs,
    navController: NavController
) {
    viewModel.initControlState()
    val state: ControlUiState by viewModel.uiStateFlow.collectAsStateWithLifecycle(WiFiUnavailable)

    ControlScreenForState(
        serviceName = controlArgs.serviceName,
        uiState = state,
        onOverviewClick = { navController.navigate(Navigation.ServicesRoute.route) }
    )
}

/**
 * Render the control UI for the service with the given [serviceName] based on the given [uiState].
 */
@Composable
private fun ControlScreenForState(
    serviceName: String,
    uiState: ControlUiState,
    onOverviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = serviceName, modifier = modifier.testTag(TAG_SERVICE_NAME))
                },
                navigationIcon = {
                    IconButton(onClick = onOverviewClick, modifier = modifier.testTag(TAG_BTN_NAV_OVERVIEW)) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        when (uiState) {
            is WiFiUnavailable -> NoWiFiAvailable(modifier = modifier.padding(innerPadding))
            is ServiceDiscovery -> LookingUpService(modifier = modifier.padding(innerPadding))
        }
    }
}

/**
 * Render the UI if no Wi-Fi connection is currently available.
 */
@Composable
private fun NoWiFiAvailable(modifier: Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .padding(5.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_wifi_unavailable),
                contentDescription = null,
                tint = MaterialTheme.colors.error,
                modifier = modifier
                    .size(64.dp)
                    .testTag(iconTag(TAG_WIFI_UNAVAILABLE))
            )
            Spacer(modifier = modifier.width(4.dp))
            Text(
                stringResource(R.string.state_wifi_unavailable),
                fontSize = 32.sp,
                color = MaterialTheme.colors.error,
                modifier = modifier.testTag(textTag(TAG_WIFI_UNAVAILABLE))
            )
        }
        Text(
            stringResource(R.string.state_wifi_unavailable_hint),
            fontSize = 20.sp,
            modifier = modifier.testTag(TAG_WIFI_UNAVAILABLE_HINT)
        )
    }
}

/**
 * Render the UI if W-Fi is available, and the service has to be looked up in the network.
 */
@Composable
private fun LookingUpService(modifier: Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .padding(5.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_wifi_available),
                contentDescription = null,
                modifier = modifier.testTag(iconTag(TAG_WIFI_AVAILABLE))
            )
            Spacer(modifier = modifier.width(4.dp))
            Text(
                stringResource(R.string.state_wifi_available),
                fontSize = 18.sp,
                modifier = modifier.testTag(textTag(TAG_WIFI_AVAILABLE))
            )
        }
    }
}

@Preview
@Composable
fun ControlScreenPreview(
    @PreviewParameter(ControlUiStatePreviewProvider::class)
    uiState: ControlUiState
) {
    WifiControlTheme {
        ControlScreenForState(serviceName = "Test service", uiState = uiState, {})
    }
}

/**
 * A [PreviewParameterProvider] implementation to get previews for different [ControlUiState] values.
 */
class ControlUiStatePreviewProvider : PreviewParameterProvider<ControlUiState> {
    override val values: Sequence<ControlUiState>
        get() = sequenceOf(WiFiUnavailable, ServiceDiscovery)
}
