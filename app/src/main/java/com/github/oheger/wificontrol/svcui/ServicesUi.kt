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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.github.oheger.wificontrol.R
import com.github.oheger.wificontrol.domain.model.PersistentService
import com.github.oheger.wificontrol.domain.model.ServiceData
import com.github.oheger.wificontrol.domain.model.ServiceDefinition
import com.github.oheger.wificontrol.ui.theme.WifiControlTheme

internal const val TAG_ERROR_HEADER = "svcErrorHeader"
internal const val TAG_ERROR_MSG = "svcErrorMsg"
internal const val TAG_LOADING_INDICATOR = "svcLoading"
internal const val TAG_SAVE_ERROR = "svcSaveError"
internal const val TAG_SAVE_ERROR_MSG = "svcSaveErrorMsg"
internal const val TAG_SERVICE_NAME = "svcName"
internal const val TAG_ACTION_DOWN = "actDown"
internal const val TAG_ACTION_REMOVE = "actRemove"
internal const val TAG_ACTION_UP = "actUp"

/**
 * Generate the tag for an UI element related to the service with the given [serviceName]. The element is identified
 * by the given [subTag].
 */
internal fun serviceTag(serviceName: String, subTag: String): String = "${serviceName}_$subTag"

/**
 * Generate the screen with the overview over all services that can be controlled.
 */
@Composable
fun ServicesScreen(viewModel: ServicesViewModel) {
    viewModel.loadServices()

    viewModel.uiStateFlow.collectAsState(ServicesUiStateLoading).value
        .let { state ->
            ServicesScreenForState(viewModel, state)
        }
}

/**
 * Generate the screen with the overview over all services that can be controlled based on the given [state].
 * Propagate user interaction to the given [viewModel].
 */
@Composable
fun ServicesScreenForState(
    viewModel: ServicesViewModel,
    state: ServicesUiState<ServicesOverviewState>,
    modifier: Modifier = Modifier
) {
    when (state) {
        is ServicesUiStateLoading ->
            ServicesLoading(modifier = modifier)

        is ServicesUiStateLoaded ->
            ServicesLoaded(viewModel = viewModel, state = state.data, modifier = modifier)

        is ServicesUiStateError ->
            ServicesError(exception = state.error, modifier = modifier)
    }
}

/**
 * Generate the screen with the overview over all services if the data about services has been loaded successfully.
 * Use the given [state] to access the data, and [viewModel] to propagate user interaction.
 */
@Composable
fun ServicesLoaded(viewModel: ServicesViewModel, state: ServicesOverviewState, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ServicesList(viewModel = viewModel, services = state.serviceData.services, modifier)

        Spacer(modifier = modifier.weight(1.0f, fill = true))
        state.updateError?.let { error ->
            Text(
                text = stringResource(id = R.string.svc_update_error),
                color = Color.Red,
                modifier = modifier.testTag(TAG_SAVE_ERROR)
            )
            Text(
                text = error.toString(),
                color = Color.Red,
                fontStyle = FontStyle.Italic,
                modifier = modifier.testTag(TAG_SAVE_ERROR_MSG)
            )
        }
    }
}

/**
 * Generate the list view with the [services] that can be controlled by this app. User interaction is propagated to
 * the given [viewModel].
 */
@Composable
fun ServicesList(viewModel: ServicesViewModel, services: List<PersistentService>, modifier: Modifier) {
    LazyColumn {
        items(services.withIndex().toList()) { (index, service) ->
            Row {
                Text(
                    text = service.serviceDefinition.name,
                    modifier = Modifier.testTag(serviceTag(service.serviceDefinition.name, TAG_SERVICE_NAME))
                )
                ServiceActions(
                    viewModel,
                    service.serviceDefinition.name,
                    index == 0,
                    index >= services.size - 1,
                    modifier
                )
            }
        }
    }
}

/**
 * Generate the actions for the service with the given [serviceName] in the list of services, taking the position
 * of this service into account as given by [isFirst], and [isLast]. The actions are represented by clickable icons; a
 * click triggers a method invocation on the given [viewModel].
 */
@Composable
fun ServiceActions(
    viewModel: ServicesViewModel,
    serviceName: String,
    isFirst: Boolean,
    isLast: Boolean,
    modifier: Modifier
) {
    if (!isFirst) {
        Icon(
            Icons.Filled.KeyboardArrowUp,
            contentDescription = null,
            modifier
                .testTag(serviceTag(serviceName, TAG_ACTION_UP))
                .clickable { viewModel.moveServiceUp(serviceName) }
        )
    }
    if (!isLast) {
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            modifier
                .testTag(serviceTag(serviceName, TAG_ACTION_DOWN))
                .clickable { viewModel.moveServiceDown(serviceName) }
        )
    }
    Icon(
        Icons.Filled.Delete,
        contentDescription = null,
        modifier
            .testTag(serviceTag(serviceName, TAG_ACTION_REMOVE))
            .clickable { viewModel.removeService(serviceName) }
    )
}

/**
 * Generate the services screen while data is still being loaded.
 */
@Composable
fun ServicesLoading(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier.testTag(TAG_LOADING_INDICATOR))
    }
}

/**
 * Generate a screen if loading of the services failed due to the given [exception].
 */
@Composable
fun ServicesError(exception: Throwable, modifier: Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.svc_load_error_title),
            color = Color.Red,
            fontSize = 30.sp,
            modifier = modifier
                .testTag(TAG_ERROR_HEADER)
                .padding(bottom = 32.dp)
                .align(Alignment.CenterHorizontally)
        )
        Text(
            text = stringResource(id = R.string.svc_load_error_details),
            color = Color.Red,
            modifier = modifier.padding(bottom = 16.dp)
        )
        Text(
            text = exception.toString(),
            color = Color.Red,
            fontStyle = FontStyle.Italic,
            modifier = modifier.testTag(TAG_ERROR_MSG)
        )
    }
}

@Preview
@Composable
fun ServicesListPreview() {
    val services = listOf(
        PersistentService(
            serviceDefinition = ServiceDefinition("Audio", "", 0, ""),
            networkTimeout = null,
            retryDelay = null,
            sendRequestInterval = null
        ),
        PersistentService(
            serviceDefinition = ServiceDefinition("Video", "", 0, ""),
            networkTimeout = null,
            retryDelay = null,
            sendRequestInterval = null
        ),
        PersistentService(
            serviceDefinition = ServiceDefinition("Disco", "", 0, ""),
            networkTimeout = null,
            retryDelay = null,
            sendRequestInterval = null
        )
    )
    val model = PreviewServicesViewModel(services)
    val state = ServicesOverviewState(
        ServiceData(services, 0),
        IllegalStateException("Error when saving services.")
    )

    WifiControlTheme {
        Column {
            ServicesScreenForState(model, ServicesUiStateLoaded(state))
        }
    }
}

@Preview
@Composable
fun ServicesLoadingPreview() {
    val model = PreviewServicesViewModel(emptyList())
    WifiControlTheme {
        ServicesScreenForState(viewModel = model, state = ServicesUiStateLoading)
    }
}

@Preview
@Composable
fun ServicesErrorPreview() {
    val exception = IllegalStateException("Something went terribly wrong :-(")
    val state = ServicesUiStateError(exception)
    val model = PreviewServicesViewModel(emptyList())
    WifiControlTheme {
        ServicesScreenForState(viewModel = model, state = state)
    }
}
