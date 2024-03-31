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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

import com.github.oheger.wificontrol.Navigation
import com.github.oheger.wificontrol.R
import com.github.oheger.wificontrol.domain.model.PersistentService
import com.github.oheger.wificontrol.domain.model.ServiceData
import com.github.oheger.wificontrol.domain.model.ServiceDefinition
import com.github.oheger.wificontrol.ui.theme.WifiControlTheme

internal const val TAG_NO_SERVICES_MSG = "svcNoServices"
internal const val TAG_SERVICE_NAME = "svcName"
internal const val TAG_ACTION_DETAILS = "actDetails"
internal const val TAG_ACTION_DOWN = "actDown"
internal const val TAG_ACTION_REMOVE = "actRemove"
internal const val TAG_ACTION_UP = "actUp"

/**
 * Generate the tag for an UI element related to the service with the given [serviceName]. The element is identified
 * by the given [subTag].
 */
internal fun serviceTag(serviceName: String, subTag: String): String = "${serviceName}_$subTag"

/**
 * Generate the screen with the overview over all services that can be controlled based on the data from the given
 * [viewModel]. Use [navController] to navigate between different screens.
 */
@Composable
fun ServicesOverviewScreen(viewModel: ServicesViewModel, navController: NavController) {
    viewModel.loadServices()

    viewModel.uiStateFlow.collectAsState(ServicesUiStateLoading).value
        .let { state ->
            ServicesOverviewScreenForState(
                state = state,
                onDetailsClick = { index ->
                    navController.navigate(
                        Navigation.ServiceDetailsRoute.forArguments(Navigation.ServiceDetailsArgs(index))
                    )
                },
                onMoveUpClick = viewModel::moveServiceUp,
                onMoveDownClick = viewModel::moveServiceDown,
                onRemoveClick = viewModel::removeService
            )
        }
}

/**
 * Generate the screen with the overview over all services that can be controlled based on the given [state].
 * Propagate user interaction to the given callback functions.
 */
@Composable
fun ServicesOverviewScreenForState(
    state: ServicesUiState<ServicesOverviewState>,
    onDetailsClick: (Int) -> Unit,
    onMoveUpClick: (String) -> Unit,
    onMoveDownClick: (String) -> Unit,
    onRemoveClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ServicesScreen(state = state) {
        ServicesLoaded(
            state = it,
            onDetailsClick = onDetailsClick,
            onMoveUpClick = onMoveUpClick,
            onMoveDownClick = onMoveDownClick,
            onRemoveClick = onRemoveClick,
            modifier = modifier
        )
    }
}

/**
 * Generate the screen with the overview over all services if the data about services has been loaded successfully.
 * Use the given [state] to access the data, and the callback functions to propagate user interaction.
 */
@Composable
fun ServicesLoaded(
    state: ServicesOverviewState,
    onDetailsClick: (Int) -> Unit,
    onMoveUpClick: (String) -> Unit,
    onMoveDownClick: (String) -> Unit,
    onRemoveClick: (String) -> Unit,
    modifier: Modifier
) {
    ServicesScreenWithSaveError(
        error = state.updateError,
        errorHintRes = R.string.svc_update_error,
        modifier = modifier
    ) {
        ServicesList(
            services = state.serviceData.services,
            onDetailsClick = onDetailsClick,
            onMoveUpClick = onMoveUpClick,
            onMoveDownClick = onMoveDownClick,
            onRemoveClick = onRemoveClick,
            modifier
        )
    }
}

/**
 * Generate the list view with the [services] that can be controlled by this app. User interaction is propagated to
 * the given callback functions.
 */
@Composable
fun ServicesList(
    services: List<PersistentService>,
    onDetailsClick: (Int) -> Unit,
    onMoveUpClick: (String) -> Unit,
    onMoveDownClick: (String) -> Unit,
    onRemoveClick: (String) -> Unit,
    modifier: Modifier
) {
    if (services.isEmpty()) {
        Text(
            text = stringResource(id = R.string.svc_no_services),
            fontSize = 14.sp,
            modifier = modifier.testTag(TAG_NO_SERVICES_MSG)
        )
    }

    LazyColumn {
        items(services.withIndex().toList()) { (index, service) ->
            Row {
                Text(
                    text = service.serviceDefinition.name,
                    modifier = Modifier.testTag(serviceTag(service.serviceDefinition.name, TAG_SERVICE_NAME))
                )
                ServiceActions(
                    service.serviceDefinition.name,
                    index,
                    index == 0,
                    index >= services.size - 1,
                    onDetailsClick,
                    onMoveUpClick,
                    onMoveDownClick,
                    onRemoveClick,
                    modifier
                )
            }
        }
    }
}

/**
 * Generate the actions for the service with the given [serviceName] and [index] in the list of services, taking the
 * position of this service into account as given by [isFirst], and [isLast]. The actions are represented by
 * clickable icons. Clicks on these icons are propagated via callback functions.
 */
@Composable
fun ServiceActions(
    serviceName: String,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onDetailsClick: (Int) -> Unit,
    onMoveUpClick: (String) -> Unit,
    onMoveDownClick: (String) -> Unit,
    onRemoveClick: (String) -> Unit,
    modifier: Modifier
) {
    if (!isFirst) {
        Icon(
            Icons.Filled.KeyboardArrowUp,
            contentDescription = null,
            modifier
                .testTag(serviceTag(serviceName, TAG_ACTION_UP))
                .clickable { onMoveUpClick(serviceName) }
        )
    }
    if (!isLast) {
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            modifier
                .testTag(serviceTag(serviceName, TAG_ACTION_DOWN))
                .clickable { onMoveDownClick(serviceName) }
        )
    }
    Icon(
        Icons.Filled.Search,
        contentDescription = null,
        modifier
            .testTag(serviceTag(serviceName, TAG_ACTION_DETAILS))
            .clickable { onDetailsClick(index) }
    )
    Icon(
        Icons.Filled.Delete,
        contentDescription = null,
        modifier
            .testTag(serviceTag(serviceName, TAG_ACTION_REMOVE))
            .clickable { onRemoveClick(serviceName) }
    )
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
    val state = ServicesOverviewState(
        ServiceData(services, 0),
        IllegalStateException("Error when saving services.")
    )

    WifiControlTheme {
        Column {
            ServicesOverviewScreenForState(ServicesUiStateLoaded(state), {}, {}, {}, {})
        }
    }
}

@Preview
@Composable
fun ServicesListEmptyPreview() {
    val state = ServicesOverviewState(
        ServiceData(emptyList(), 0)
    )

    WifiControlTheme {
        Column {
            ServicesOverviewScreenForState(ServicesUiStateLoaded(state), {}, {}, {}, {})
        }
    }
}

@Preview
@Composable
fun ServicesLoadingPreview() {
    WifiControlTheme {
        ServicesOverviewScreenForState(state = ServicesUiStateLoading, {}, {}, {}, {})
    }
}

@Preview
@Composable
fun ServicesErrorPreview() {
    val exception = IllegalStateException("Something went terribly wrong :-(")
    val state = ServicesUiStateError(exception)
    WifiControlTheme {
        ServicesOverviewScreenForState(state = state, {}, {}, {}, {})
    }
}
