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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

import com.github.oheger.wificontrol.Navigation
import com.github.oheger.wificontrol.R
import com.github.oheger.wificontrol.domain.model.PersistentService
import com.github.oheger.wificontrol.domain.model.ServiceData
import com.github.oheger.wificontrol.domain.model.ServiceDefinition
import com.github.oheger.wificontrol.ui.theme.WifiControlTheme

internal const val TAG_NO_SERVICES_MSG = "svcNoServices"
internal const val TAG_SERVICE_NAME = "svcName"
internal const val TAG_ACTION_CREATE = "actCreate"
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
    val state: ServicesUiState<ServicesOverviewState> by viewModel.uiStateFlow.collectAsStateWithLifecycle(
        ServicesUiStateLoading
    )

    ServicesOverviewScreenForState(
        state = state,
        onServiceClick = { serviceName ->
            navController.navigate(
                Navigation.ControlServiceRoute.forArguments(Navigation.ControlServiceArgs(serviceName))
            )
        },
        onDetailsClick = { index ->
            navController.navigate(
                Navigation.ServiceDetailsRoute.forArguments(Navigation.ServiceDetailsArgs(index))
            )
        },
        onMoveUpClick = viewModel::moveServiceUp,
        onMoveDownClick = viewModel::moveServiceDown,
        onRemoveClick = viewModel::removeService,
        onCreateClick = { navController.navigate(Navigation.ServiceDetailsRoute.forNewService) }
    )
}

/**
 * Generate the screen with the overview over all services that can be controlled based on the given [state].
 * Propagate user interaction to the given callback functions.
 */
@Composable
fun ServicesOverviewScreenForState(
    state: ServicesUiState<ServicesOverviewState>,
    onServiceClick: (String) -> Unit,
    onDetailsClick: (Int) -> Unit,
    onMoveUpClick: (String) -> Unit,
    onMoveDownClick: (String) -> Unit,
    onRemoveClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ServicesScreen(state = state) { overviewState ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = stringResource(id = R.string.svc_overview_title))
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onCreateClick, modifier = modifier.testTag(TAG_ACTION_CREATE)) {
                    Icon(Icons.Filled.Add, null, modifier = modifier)
                }
            }
        ) { innerPadding ->
            ServicesLoaded(
                state = overviewState,
                onServiceClick = onServiceClick,
                onDetailsClick = onDetailsClick,
                onMoveUpClick = onMoveUpClick,
                onMoveDownClick = onMoveDownClick,
                onRemoveClick = onRemoveClick,
                modifier = modifier.padding(innerPadding)
            )
        }
    }
}

/**
 * Generate the screen with the overview over all services if the data about services has been loaded successfully.
 * Use the given [state] to access the data, and the callback functions to propagate user interaction.
 */
@Composable
fun ServicesLoaded(
    state: ServicesOverviewState,
    onServiceClick: (String) -> Unit,
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
            onServiceClick = onServiceClick,
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
    onServiceClick: (String) -> Unit,
    onDetailsClick: (Int) -> Unit,
    onMoveUpClick: (String) -> Unit,
    onMoveDownClick: (String) -> Unit,
    onRemoveClick: (String) -> Unit,
    modifier: Modifier
) {
    if (services.isEmpty()) {
        Text(
            text = stringResource(id = R.string.svc_no_services),
            fontSize = 16.sp,
            modifier = modifier.testTag(TAG_NO_SERVICES_MSG)
        )
    }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(services.withIndex().toList()) { (index, service) ->
            val color = if (index % 2 == 0) MaterialTheme.colors.surface else MaterialTheme.colors.secondary
            Row(
                modifier = modifier
                    .background(color)
                    .padding(top = 8.dp, bottom = 8.dp)
            ) {
                Text(
                    text = service.serviceDefinition.name,
                    fontSize = 24.sp,
                    modifier = Modifier
                        .clickable { onServiceClick(service.serviceDefinition.name) }
                        .testTag(serviceTag(service.serviceDefinition.name, TAG_SERVICE_NAME))
                )
                Spacer(modifier = modifier.weight(1f))
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

        // Add space at the bottom of the list to prevent that the floating button blocks the actions of the last
        // list elements. With this space, the elements can be scrolled until they are above the button.
        item {
            Spacer(modifier = modifier.height(54.dp))
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
    Row(
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        if (!isFirst) {
            ServiceAction(
                image = Icons.Filled.KeyboardArrowUp,
                onClick = { onMoveUpClick(serviceName) },
                tag = serviceTag(serviceName, TAG_ACTION_UP),
                modifier = modifier
            )
        }
        if (!isLast) {
            ServiceAction(
                image = Icons.Filled.KeyboardArrowDown,
                onClick = { onMoveDownClick(serviceName) },
                tag = serviceTag(serviceName, TAG_ACTION_DOWN),
                modifier = modifier
            )
        }
        ServiceAction(
            image = Icons.Filled.Search,
            onClick = { onDetailsClick(index) },
            tag = serviceTag(serviceName, TAG_ACTION_DETAILS),
            modifier = modifier
        )
        ServiceAction(
            image = Icons.Filled.Delete,
            onClick = { onRemoveClick(serviceName) },
            tag = serviceTag(serviceName, TAG_ACTION_REMOVE),
            modifier = modifier
        )
    }
}

/**
 * Generate an icon with the given [image] to represent an action to modify a service. Use the given [onClick]
 * function to report clicks and set the given [tag] to support testing.
 */
@Composable
private fun ServiceAction(image: ImageVector, onClick: () -> Unit, tag: String, modifier: Modifier) {
    Icon(
        image,
        contentDescription = null,
        modifier = modifier
            .testTag(tag)
            .size(34.dp)
            .clickable(onClick = onClick)
    )
}

@Preview
@Composable
fun ServicesListPreview() {
    val services = listOf(
        PersistentService(
            serviceDefinition = ServiceDefinition("Audio", "", 0, ""),
            lookupTimeout = null,
            sendRequestInterval = null
        ),
        PersistentService(
            serviceDefinition = ServiceDefinition("Video", "", 0, ""),
            lookupTimeout = null,
            sendRequestInterval = null
        ),
        PersistentService(
            serviceDefinition = ServiceDefinition("Disco", "", 0, ""),
            lookupTimeout = null,
            sendRequestInterval = null
        )
    )
    val state = ServicesOverviewState(
        ServiceData(services, 0),
        IllegalStateException("Error when saving services.")
    )

    WifiControlTheme {
        Column {
            ServicesOverviewScreenForState(ServicesUiStateLoaded(state), {}, {}, {}, {}, {}, {})
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
            ServicesOverviewScreenForState(ServicesUiStateLoaded(state), {}, {}, {}, {}, {}, {})
        }
    }
}

@Preview
@Composable
fun ServicesLoadingPreview() {
    WifiControlTheme {
        ServicesOverviewScreenForState(state = ServicesUiStateLoading, {}, {}, {}, {}, {}, {})
    }
}

@Preview
@Composable
fun ServicesErrorPreview() {
    val exception = IllegalStateException("Something went terribly wrong :-(")
    val state = ServicesUiStateError(exception)
    WifiControlTheme {
        ServicesOverviewScreenForState(state = state, {}, {}, {}, {}, {}, {})
    }
}
