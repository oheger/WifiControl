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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

import com.github.oheger.wificontrol.Navigation
import com.github.oheger.wificontrol.R
import com.github.oheger.wificontrol.domain.model.PersistentService
import com.github.oheger.wificontrol.domain.model.ServiceData
import com.github.oheger.wificontrol.domain.model.ServiceDefinition
import com.github.oheger.wificontrol.ui.theme.WifiControlTheme

internal const val TAG_SHOW_NAME = "svcShowName"
internal const val TAG_SHOW_MULTICAST = "svcShowMulticast"
internal const val TAG_SHOW_PORT = "svcShowPort"
internal const val TAG_SHOW_CODE = "svcShowCode"

internal const val TAG_EDIT_NAME = "svcEditName"
internal const val TAG_EDIT_MULTICAST = "svcEditMulticast"
internal const val TAG_EDIT_PORT = "svcEditPort"
internal const val TAG_EDIT_CODE = "svcEditCode"
internal const val TAG_BTN_EDIT_SERVICE = "svcBtnEdit"
internal const val TAG_BTN_EDIT_CANCEL = "svcBtnCancel"
internal const val TAG_BTN_EDIT_SAVE = "svcBtnSave"
internal const val TAG_BTN_SVC_OVERVIEW = "svcBtnOverview"
internal const val TAG_SVC_TITLE = "svcDetailsTitle"

/** The indent of the property value relative to the associated label. */
internal const val PROPERTY_INDENT = 10

/**
 * Generate the screen showing the details of a specific service, which also allows editing the service. Use the given
 * [viewModel] to load and access the data to be displayed. Process the service identified by the given
 * [serviceDetailsArgs]. Use the given [navController] for navigation to other screens if required.
 */
@Composable
fun ServiceDetailsScreen(
    viewModel: ServiceDetailsViewModel,
    serviceDetailsArgs: Navigation.ServiceDetailsArgs,
    navController: NavController
) {
    viewModel.loadService(serviceDetailsArgs.serviceIndex)

    viewModel.uiStateFlow.collectAsState(ServicesUiStateLoading).value
        .let { state ->
            ServiceDetailsScreenForState(
                state = state,
                onEditClick = viewModel::editService,
                onSaveClick = { service -> viewModel.saveService(service, navController) },
                onCancelClick = { viewModel.cancelEdit(navController) },
                onOverviewClick = { navController.navigate(Navigation.ServicesRoute.route) }
            )
        }
}

/**
 * Generate the screen for the details of a service based on the given [state]. Use the given callback functions to
 * propagate user interaction.
 */
@Composable
private fun ServiceDetailsScreenForState(
    state: ServicesUiState<ServiceDetailsState>,
    onEditClick: () -> Unit,
    onSaveClick: (PersistentService) -> Unit,
    onCancelClick: () -> Unit,
    onOverviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ServicesScreen(state = state) { detailsState ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (detailsState.serviceIndex == ServiceData.NEW_SERVICE_INDEX) {
                                stringResource(id = R.string.svc_new_title)
                            } else {
                                detailsState.service.serviceDefinition.name
                            },
                            modifier = modifier.testTag(TAG_SVC_TITLE)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onOverviewClick, modifier = modifier.testTag(TAG_BTN_SVC_OVERVIEW)) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                )
            }
        ) { innerPadding ->
            ServiceDetails(
                state = detailsState,
                onEditClick = onEditClick,
                onSaveClick = onSaveClick,
                onCancelClick = onCancelClick,
                modifier = modifier.padding(innerPadding)
            )
        }
    }
}

/**
 * Generate the screen for the details of a service either in view or edit mode, depending on the given [state].
 * Use the given callback functions to propagate user interaction.
 */
@Composable
private fun ServiceDetails(
    state: ServiceDetailsState,
    onEditClick: () -> Unit,
    onSaveClick: (PersistentService) -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier
) {
    if (state.editMode) {
        ServicesScreenWithSaveError(
            error = state.saveError,
            errorHintRes = R.string.svc_save_service_error,
            modifier = modifier
        ) {
            EditServiceDetails(
                service = state.service,
                onSaveClick = onSaveClick,
                onCancelClick = onCancelClick,
                modifier = modifier
            )
        }
    } else {
        ViewServiceDetails(service = state.service, onEditClick = onEditClick, modifier = modifier)
    }
}

/**
 * Generate the UI to view the details of the given [service]. Use the given [onEditClick] callback to indicate that
 * the user wants to edit this service.
 */
@Composable
private fun ViewServiceDetails(service: PersistentService, onEditClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ServiceProperty(
            labelRes = R.string.svc_lab_name,
            value = service.serviceDefinition.name,
            tag = TAG_SHOW_NAME,
            modifier = modifier
        )
        ServiceProperty(
            labelRes = R.string.svc_lab_multicast,
            value = service.serviceDefinition.multicastAddress,
            tag = TAG_SHOW_MULTICAST,
            modifier = modifier
        )
        ServiceProperty(
            labelRes = R.string.svc_lab_port,
            value = service.serviceDefinition.port.toString(),
            tag = TAG_SHOW_PORT,
            modifier = modifier
        )
        ServiceProperty(
            labelRes = R.string.svc_lab_code,
            value = service.serviceDefinition.requestCode,
            tag = TAG_SHOW_CODE,
            modifier = modifier
        )
        Button(
            onClick = onEditClick,
            modifier = modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 10.dp)
                .testTag(TAG_BTN_EDIT_SERVICE)
        ) {
            Text(text = stringResource(id = R.string.svc_btn_edit))
        }
    }
}

/**
 * Generate the UI to edit the properties of the given [service]. Use the [onSaveClick] and [onCancelClick] callbacks
 * to report that the user clicked on the save or cancel button respectively.
 */
@Composable
private fun EditServiceDetails(
    service: PersistentService,
    onSaveClick: (PersistentService) -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier
) {
    var name by rememberSaveable { mutableStateOf(service.serviceDefinition.name) }
    var multicast by rememberSaveable { mutableStateOf(service.serviceDefinition.multicastAddress) }
    var port by rememberSaveable {
        mutableStateOf(service.serviceDefinition.port.takeIf { it > 0 }?.toString().orEmpty())
    }
    var code by rememberSaveable { mutableStateOf(service.serviceDefinition.requestCode) }

    fun createEditedService(): PersistentService =
        PersistentService(
            serviceDefinition = ServiceDefinition(
                name = name,
                multicastAddress = multicast,
                port = port.toInt(),
                requestCode = code
            ),
            networkTimeout = null,
            retryDelay = null,
            sendRequestInterval = null
        )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        EditServiceProperty(
            labelRes = R.string.svc_lab_name,
            value = name,
            updateValue = { name = it },
            tag = TAG_EDIT_NAME,
            modifier = modifier
        )
        EditServiceProperty(
            labelRes = R.string.svc_lab_multicast,
            value = multicast,
            updateValue = { multicast = it },
            tag = TAG_EDIT_MULTICAST,
            modifier = modifier
        )
        EditServiceProperty(
            labelRes = R.string.svc_lab_port,
            value = port,
            updateValue = { port = it },
            tag = TAG_EDIT_PORT,
            modifier = modifier
        )
        EditServiceProperty(
            labelRes = R.string.svc_lab_code,
            value = code,
            updateValue = { code = it },
            tag = TAG_EDIT_CODE,
            modifier = modifier
        )

        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = modifier
                .padding(top = 15.dp)
                .fillMaxWidth()
        ) {
            Button(
                onClick = { onSaveClick(createEditedService()) },
                modifier = modifier.testTag(TAG_BTN_EDIT_SAVE)
            ) {
                Text(text = stringResource(id = R.string.svc_btn_save))
            }
            Button(onClick = onCancelClick, modifier = modifier.testTag(TAG_BTN_EDIT_CANCEL)) {
                Text(text = stringResource(id = R.string.svc_btn_cancel))
            }
        }
    }
}

/**
 * Generate the UI to show a single service property consisting of a label with the given [resource ID][labelRes] and
 * the given [value] of the property. Assign the given [tag] to the text with the value.
 */
@Composable
private fun ServiceProperty(labelRes: Int, value: String, tag: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 15.dp)
    ) {
        PropertyLabel(labelRes = labelRes, modifier = modifier)
        Text(
            text = value,
            modifier = modifier
                .testTag(tag)
                .padding(start = PROPERTY_INDENT.dp)
        )
    }
}

/**
 * Generate the UI to edit a single property of a service. This contains a label with the given
 * [resource ID][labelRes], and a text field displaying the given [value] and using the [updateValue] function to
 * propagate changes. Assign the given [tag] to the edit text field.
 */
@Composable
private fun EditServiceProperty(
    labelRes: Int,
    value: String,
    updateValue: (String) -> Unit,
    tag: String,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 15.dp)
    ) {
        PropertyLabel(labelRes = labelRes, modifier = modifier)
        TextField(
            value = value,
            onValueChange = updateValue,
            modifier = modifier
                .testTag(tag)
                .padding(start = PROPERTY_INDENT.dp)
        )
    }
}

/**
 * Generate the label for a service property. The label text is defined by the given [resource ID][labelRes].
 */
@Composable
private fun PropertyLabel(labelRes: Int, modifier: Modifier) {
    Text(text = stringResource(id = labelRes), fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = modifier)
}

@Preview
@Composable
fun ViewServiceDetailsPreview() {
    val service = PersistentService(
        serviceDefinition = ServiceDefinition(
            name = "Audio Service",
            multicastAddress = "231.1.2.3",
            port = 7777,
            requestCode = "Lookup_audio_service"
        ),
        networkTimeout = null,
        retryDelay = null,
        sendRequestInterval = null
    )
    val serviceData = ServiceData(emptyList(), 0)
    val state = ServicesUiStateLoaded(ServiceDetailsState(serviceData, 0, service, editMode = false))

    WifiControlTheme {
        ServiceDetailsScreenForState(state = state, {}, {}, {}, {})
    }
}

@Preview
@Composable
fun EditServiceDetailsPreview() {
    val service = PersistentService(
        serviceDefinition = ServiceDefinition(
            name = "Video Service",
            multicastAddress = "231.4.3.2",
            port = 8888,
            requestCode = "Find_Video_Service"
        ),
        networkTimeout = null,
        retryDelay = null,
        sendRequestInterval = null
    )
    val serviceData = ServiceData(emptyList(), 0)
    val saveException = IllegalStateException("Could not save service.")
    val detailsState = ServiceDetailsState(serviceData, 0, service, editMode = true, saveException)
    val state = ServicesUiStateLoaded(detailsState)

    WifiControlTheme {
        ServiceDetailsScreenForState(state = state, {}, {}, {}, {})
    }
}
