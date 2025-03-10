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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

import com.github.oheger.wificontrol.Navigation
import com.github.oheger.wificontrol.R
import com.github.oheger.wificontrol.domain.model.PersistentService
import com.github.oheger.wificontrol.domain.model.ServiceAddressMode
import com.github.oheger.wificontrol.domain.model.ServiceData
import com.github.oheger.wificontrol.domain.model.ServiceDefinition
import com.github.oheger.wificontrol.ui.theme.WifiControlTheme

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal const val TAG_SHOW_NAME = "svcShowName"
internal const val TAG_SHOW_MULTICAST = "svcShowMulticast"
internal const val TAG_SHOW_PORT = "svcShowPort"
internal const val TAG_SHOW_CODE = "svcShowCode"
internal const val TAG_SHOW_URL = "svcShowUrl"
internal const val TAG_SHOW_LOOKUP_TIMEOUT = "svcShowLookupTimeout"
internal const val TAG_SHOW_REQUEST_INTERVAL = "svcShowRequestInterval"

internal const val TAG_EDIT_NAME = "svcEditName"
internal const val TAG_EDIT_MULTICAST = "svcEditMulticast"
internal const val TAG_EDIT_PORT = "svcEditPort"
internal const val TAG_EDIT_CODE = "svcEditCode"
internal const val TAG_EDIT_LOOKUP_TIMEOUT = "svcEditLookupTimeout"
internal const val TAG_EDIT_REQUEST_INTERVAL = "svcEditRequestInterval"
internal const val TAG_EDIT_SERVICE_URL = "svcEditUrl"
internal const val TAG_EDIT_URL_PROVIDED = "svcEditUrlProvided"

internal const val TAG_BTN_CONTROL_SERVICE = "svcBtnControl"
internal const val TAG_BTN_EDIT_CANCEL = "svcBtnCancel"
internal const val TAG_BTN_EDIT_SERVICE = "svcBtnEdit"
internal const val TAG_BTN_EDIT_SAVE = "svcBtnSave"
internal const val TAG_BTN_SVC_OVERVIEW = "svcBtnOverview"

internal const val TAG_SVC_TITLE = "svcDetailsTitle"

/** The indent of the property value relative to the associated label. */
internal const val PROPERTY_INDENT = 10

/**
 * Generate the tag for an error text associated for the input field with the given [tag].
 */
internal fun errorTag(tag: String): String = "${tag}Error"

/**
 * Generate the tag for a switch component that determines whether for a specific property identified by [tag] the
 * default value should be used. In this case, the user does not have to enter a value for this property.
 */
internal fun useDefaultTag(tag: String): String = "${tag}UseDefault"

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
    viewModel.loadUiState(ServiceDetailsViewModel.Parameters(serviceDetailsArgs.serviceIndex))
    val state: ServicesUiState<ServiceDetailsState> by
    viewModel.uiStateFlow.collectAsStateWithLifecycle(ServicesUiStateLoading)

    ServiceDetailsScreenForState(
        state = state,
        editModelFunc = { viewModel.editModel },
        onEditClick = viewModel::editService,
        onControlClick = { navController.navigateControl(state) },
        onSaveClick = { service -> viewModel.saveService(service, navController) },
        onCancelClick = { viewModel.cancelEdit(navController) },
        onOverviewClick = { navController.navigate(Navigation.ServicesRoute.route) }
    )
}

/**
 * Generate the screen for the details of a service based on the given [state]. Use the given callback functions to
 * propagate user interaction. Use the given [editModelFunc] to obtain a model for editing the current service on
 * demand.
 */
@Composable
private fun ServiceDetailsScreenForState(
    state: ServicesUiState<ServiceDetailsState>,
    editModelFunc: () -> ServiceEditModel,
    onEditClick: () -> Unit,
    onControlClick: () -> Unit,
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
                    },
                    actions = {
                        if (!detailsState.editMode) {
                            IconButton(onClick = onEditClick, modifier = modifier.testTag(TAG_BTN_EDIT_SERVICE)) {
                                Icon(imageVector = Icons.Filled.Edit, contentDescription = null)
                            }
                            IconButton(
                                onClick = onControlClick,
                                modifier = modifier.testTag(TAG_BTN_CONTROL_SERVICE)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_remote_control),
                                    contentDescription = null
                                )
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            ServiceDetails(
                state = detailsState,
                editModelFunc = editModelFunc,
                onSaveClick = onSaveClick,
                onCancelClick = onCancelClick,
                modifier = modifier.padding(innerPadding)
            )
        }
    }
}

/**
 * Generate the screen for the details of a service either in view or edit mode, depending on the given [state].
 * In case of edit mode, use the given [editModelFunc] to obtain a model to track and validate user input. Use the
 * given callback functions* to propagate user interaction.
 */
@Composable
private fun ServiceDetails(
    state: ServiceDetailsState,
    editModelFunc: () -> ServiceEditModel,
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
                editModelFunc = editModelFunc,
                onSaveClick = onSaveClick,
                onCancelClick = onCancelClick,
                modifier = modifier
            )
        }
    } else {
        ViewServiceDetails(service = state.service, modifier = modifier)
    }
}

/**
 * Generate the UI to view the details of the given [service].
 */
@Composable
private fun ViewServiceDetails(service: PersistentService, modifier: Modifier) {
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

        when(service.serviceDefinition.addressMode) {
            ServiceAddressMode.WIFI_DISCOVERY -> ViewDiscoveryDetails(service, modifier)
            ServiceAddressMode.FIX_URL -> ViewFixUrlProperties(service, modifier)
        }
    }
}

/**
 * Generate the UI with the service details related to discovery for the given [service].
 */
@Composable
private fun ViewDiscoveryDetails(service: PersistentService, modifier: Modifier) {
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
    ServiceDurationProperty(
        labelRes = R.string.svc_lab_lookup_timeout,
        value = service.lookupTimeout?.inWholeSeconds,
        unitRes = R.string.svc_unit_sec,
        tag = TAG_SHOW_LOOKUP_TIMEOUT,
        modifier = modifier
    )
    ServiceDurationProperty(
        labelRes = R.string.svc_lab_request_interval,
        value = service.sendRequestInterval?.inWholeMilliseconds,
        unitRes = R.string.svc_unit_ms,
        tag = TAG_SHOW_REQUEST_INTERVAL,
        modifier = modifier
    )
}

/**
 * Generate the UI with the service details related to the provided URL for the given [service].
 */
@Composable
private fun ViewFixUrlProperties(service: PersistentService, modifier: Modifier) {
    ServiceProperty(
        labelRes = R.string.svc_lab_url,
        value = service.serviceDefinition.serviceUrl,
        tag = TAG_SHOW_URL,
        modifier = modifier
    )
}

/**
 * Generate the UI to edit the properties of the current service using a [ServiceEditModel] that can be obtained via
 * the given [editModelFunc] function. Use the [onSaveClick] and [onCancelClick] callbacks to report that the user
 * clicked on the save or cancel button respectively.
 */
@Composable
private fun EditServiceDetails(
    editModelFunc: () -> ServiceEditModel,
    onSaveClick: (PersistentService) -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier
) {
    val editModel = editModelFunc()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        EditServiceProperty(
            labelRes = R.string.svc_lab_name,
            keyboardOptions = KeyboardOptions.Default,
            value = editModel.serviceName,
            updateValue = { editModel.serviceName = it },
            errorRes = R.string.svc_name_invalid.takeUnless { editModel.serviceNameValid },
            tag = TAG_EDIT_NAME,
            modifier = modifier
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(top = 15.dp)) {
            PropertyLabel(R.string.svc_lab_url_provided, modifier)
            Spacer(modifier = modifier.weight(1f))
            Switch(
                checked = editModel.isServiceUrlProvided,
                onCheckedChange = { it: Boolean -> editModel.isServiceUrlProvided = it },
                modifier = modifier.testTag(tag = TAG_EDIT_URL_PROVIDED)
            )
        }
        HelpText(R.string.svc_help_address_mode, modifier)

        if (editModel.isServiceUrlProvided) {
            EditFixUrlProperties(editModel, modifier)
        } else {
            EditDiscoveryProperties(editModel, modifier)
        }

        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = modifier
                .padding(top = 15.dp)
                .fillMaxWidth()
        ) {
            Button(
                onClick = {
                    if (editModel.validate()) {
                        onSaveClick(editModel.editedService())
                    }
                },
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
 * Generate the UI for editing the properties of a service related to service discovery based on the given [editModel].
 */
@Composable
private fun EditDiscoveryProperties(editModel: ServiceEditModel, modifier: Modifier) {
    EditServiceProperty(
        labelRes = R.string.svc_lab_multicast,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        value = editModel.multicastAddress,
        updateValue = { editModel.multicastAddress = it },
        errorRes = R.string.svc_address_invalid.takeUnless { editModel.multicastAddressValid },
        tag = TAG_EDIT_MULTICAST,
        helpRes = R.string.svc_help_multicast,
        modifier = modifier
    )
    EditServiceProperty(
        labelRes = R.string.svc_lab_port,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        value = editModel.port,
        updateValue = { editModel.port = it },
        errorRes = R.string.svc_port_invalid.takeUnless { editModel.portValid },
        tag = TAG_EDIT_PORT,
        helpRes = R.string.svc_help_port,
        modifier = modifier
    )
    EditServiceProperty(
        labelRes = R.string.svc_lab_code,
        keyboardOptions = KeyboardOptions.Default,
        value = editModel.code,
        updateValue = { editModel.code = it },
        errorRes = R.string.svc_code_invalid.takeUnless { editModel.codeValid },
        tag = TAG_EDIT_CODE,
        helpRes = R.string.svc_help_code,
        modifier = modifier
    )
    EditServicePropertyWithDefault(
        labelRes = R.string.svc_lab_lookup_timeout,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        useDefault = editModel.lookupTimeoutDefault,
        updateUseDefault = { editModel.lookupTimeoutDefault = it },
        value = editModel.lookupTimeoutSec,
        updateValue = { editModel.lookupTimeoutSec = it },
        errorRes = R.string.svc_lookup_timeout_invalid.takeUnless { editModel.lookupTimeoutValid },
        tag = TAG_EDIT_LOOKUP_TIMEOUT,
        helpRes = R.string.svc_help_timeout,
        modifier = modifier
    )
    EditServicePropertyWithDefault(
        labelRes = R.string.svc_lab_request_interval,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        useDefault = editModel.sendRequestIntervalDefault,
        updateUseDefault = { editModel.sendRequestIntervalDefault = it },
        value = editModel.sendRequestIntervalMs,
        updateValue = { editModel.sendRequestIntervalMs = it },
        errorRes = R.string.svc_request_interval_invalid.takeUnless { editModel.sendRequestIntervalValid },
        tag = TAG_EDIT_REQUEST_INTERVAL,
        helpRes = R.string.svc_help_request_interval,
        modifier = modifier
    )
}

/**
 * Generate the UI for editing the properties of a service related to a fix URL based on the given [editModel].
 */
@Composable
private fun EditFixUrlProperties(editModel: ServiceEditModel, modifier: Modifier) {
    EditServiceProperty(
        labelRes = R.string.svc_lab_url,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        value = editModel.serviceUrl,
        updateValue = { editModel.serviceUrl = it },
        errorRes = R.string.svc_url_invalid.takeUnless { editModel.serviceUrlValid },
        tag = TAG_EDIT_SERVICE_URL,
        modifier = modifier
    )
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
 * Generate the UI to show an optional service property for a duration with the given [resource ID][labelRes], the
 * given [value], and the resource ID for the [unit][unitRes]. Assign the given [tag] to the text with the value.
 */
@Composable
private fun ServiceDurationProperty(labelRes: Int, value: Long?, unitRes: Int, tag: String, modifier: Modifier) {
    value?.let { durationValue ->
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 15.dp)
        ) {
            PropertyLabel(labelRes = labelRes, modifier = modifier)
            Row(modifier = modifier.padding(start = PROPERTY_INDENT.dp)) {
                Text(
                    text = durationValue.toString(),
                    modifier = modifier.testTag(tag)
                )
                Text(
                    text = stringResource(unitRes),
                    modifier = modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

/**
 * Generate the UI to edit a single property of a service. This contains a label with the given
 * [resource ID][labelRes], and a text field displaying the given [value] and using the [updateValue] function to
 * propagate changes. The input can be edited with a keyboard corresponding to the provided [keyboardOptions]. If the
 * user input is invalid, a resource ID with a corresponding error message is provided in [errorRes]; then display this
 * message. Assign the given [tag] to the edit text field. Display an optional help text as defined by [helpRes].
 */
@Composable
private fun EditServiceProperty(
    labelRes: Int,
    keyboardOptions: KeyboardOptions,
    value: String,
    updateValue: (String) -> Unit,
    errorRes: Int?,
    tag: String,
    helpRes: Int? = null,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 15.dp)
    ) {
        PropertyLabel(labelRes = labelRes, modifier = modifier)
        HelpText(helpRes, modifier)
        EditField(keyboardOptions, value, updateValue, errorRes, tag, modifier.padding(top = 5.dp))
    }
}

/**
 * Generate the UI to edit a service property that supports a default value. This is analogous to
 * [EditServiceProperty], but there is an additional switch controlled by [useDefault] and [updateUseDefault] that
 * determines whether the default value is selected or not.
 */
@Composable
private fun EditServicePropertyWithDefault(
    labelRes: Int,
    keyboardOptions: KeyboardOptions,
    useDefault: Boolean,
    updateUseDefault: (Boolean) -> Unit,
    value: String,
    updateValue: (String) -> Unit,
    errorRes: Int?,
    tag: String,
    helpRes: Int?,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 15.dp)
    ) {
        PropertyLabel(labelRes = labelRes, modifier = modifier)
        HelpText(helpRes, modifier)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(start = PROPERTY_INDENT.dp)) {
            Text(stringResource(R.string.svc_use_default))
            Spacer(modifier = modifier.weight(1f))
            Switch(
                checked = useDefault,
                onCheckedChange = updateUseDefault,
                modifier = modifier.testTag(useDefaultTag(tag))
            )
        }

        if (!useDefault) {
            EditField(keyboardOptions, value, updateValue, errorRes, tag, modifier)
        }
    }
}

/**
 * Generate an optional help text based on the given [string resource][helpRes].
 */
@Composable
private fun HelpText(helpRes: Int?, modifier: Modifier) {
    helpRes?.let {
        Text(
            text = stringResource(it),
            fontSize = 12.sp,
            lineHeight = 15.sp,
            modifier = modifier.padding(start = PROPERTY_INDENT.dp)
        )
    }
}

/**
 * Generate a text field for editing the service property identified by [tag]. Allow configuring [keyboardOptions].
 * Use the given [value] for the field and the [updateValue] function to report changes. If [errorRes] is not *null*,
 * render a validation error message.
 */
@Composable
private fun EditField(
    keyboardOptions: KeyboardOptions,
    value: String,
    updateValue: (String) -> Unit,
    errorRes: Int?,
    tag: String,
    modifier: Modifier
) {
    TextField(
        value = value,
        onValueChange = updateValue,
        keyboardOptions = keyboardOptions,
        modifier = modifier
            .testTag(tag)
            .padding(start = PROPERTY_INDENT.dp)
    )
    errorRes?.let { id ->
        Text(
            text = stringResource(id),
            color = MaterialTheme.colors.error,
            modifier = modifier
                .testTag(errorTag(tag))
                .padding(start = PROPERTY_INDENT.dp, top = 8.dp)
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

/**
 * Helper function to navigate to the control UI for the current service in the given [state] object.
 */
private fun NavController.navigateControl(state: ServicesUiState<ServiceDetailsState>) {
    val serviceName = state.process { it.service.serviceDefinition.name }.orEmpty()
    navigate(Navigation.ControlServiceRoute.forArguments(Navigation.ControlServiceArgs(serviceName)))
}

@Preview
@Composable
fun ViewServiceDetailsPreview() {
    val service = PersistentService(
        serviceDefinition = ServiceDefinition(
            name = "Audio Service",
            addressMode = ServiceAddressMode.WIFI_DISCOVERY,
            multicastAddress = "231.1.2.3",
            port = 7777,
            requestCode = "Lookup_audio_service",
            serviceUrl = ""
        ),
        lookupTimeout = null,
        sendRequestInterval = 50.milliseconds
    )
    val serviceData = ServiceData(emptyList())
    val editModel = ServiceEditModel(service)
    val state = ServicesUiStateLoaded(ServiceDetailsState(serviceData, 0, service, editMode = false))

    WifiControlTheme {
        ServiceDetailsScreenForState(state = state, { editModel }, {}, {}, {}, {}, {})
    }
}

@Preview
@Composable
fun ViewServiceWithUrlDetailsPreview() {
    val service = PersistentService(
        serviceDefinition = ServiceDefinition(
            name = "URL Service",
            addressMode = ServiceAddressMode.FIX_URL,
            multicastAddress = "",
            port = 0,
            requestCode = "",
            serviceUrl = "https://192.168.21.22/test/index.htm"
        ),
        lookupTimeout = null,
        sendRequestInterval = null
    )
    val serviceData = ServiceData(emptyList())
    val editModel = ServiceEditModel(service)
    val state = ServicesUiStateLoaded(ServiceDetailsState(serviceData, 0, service, editMode = false))

    WifiControlTheme {
        ServiceDetailsScreenForState(state = state, { editModel }, {}, {}, {}, {}, {})
    }
}

@Preview
@Composable
fun EditServiceDetailsPreview() {
    val service = PersistentService(
        serviceDefinition = ServiceDefinition(
            name = "Video Service ",
            addressMode = ServiceAddressMode.WIFI_DISCOVERY,
            multicastAddress = "231.4.3.2.5",
            port = 8888,
            requestCode = "Find_Video_Service",
            serviceUrl = ""
        ),
        lookupTimeout = null,
        sendRequestInterval = 0.seconds
    )
    val serviceData = ServiceData(emptyList())
    val editModel = ServiceEditModel(service)
    editModel.validate()
    val saveException = IllegalStateException("Could not save service.")
    val detailsState = ServiceDetailsState(serviceData, 0, service, editMode = true, saveException)
    val state = ServicesUiStateLoaded(detailsState)

    WifiControlTheme {
        ServiceDetailsScreenForState(state = state, { editModel }, {}, {}, {}, {}, {})
    }
}

@Preview
@Composable
fun EditServiceWithUrlPreview() {
    val service = PersistentService(
        serviceDefinition = ServiceDefinition(
            name = "URL Service",
            addressMode = ServiceAddressMode.FIX_URL,
            multicastAddress = "",
            port = 0,
            requestCode = "",
            serviceUrl = "https:// not a URL"
        ),
        lookupTimeout = null,
        sendRequestInterval = null
    )
    val serviceData = ServiceData(emptyList())
    val editModel = ServiceEditModel(service)
    editModel.validate()
    val saveException = IllegalStateException("Could not save service.")
    val detailsState = ServiceDetailsState(serviceData, 0, service, editMode = true, saveException)
    val state = ServicesUiStateLoaded(detailsState)

    WifiControlTheme {
        ServiceDetailsScreenForState(state = state, { editModel }, {}, {}, {}, {}, {})
    }
}

@Preview
@Composable
fun EditServicePropertyWithDefaultPreview() {
    WifiControlTheme {
        EditServicePropertyWithDefault(
            labelRes = R.string.svc_lab_lookup_timeout,
            keyboardOptions = KeyboardOptions.Default,
            useDefault = false,
            updateUseDefault = {},
            value = "55",
            updateValue = {},
            errorRes = R.string.svc_lookup_timeout_invalid,
            tag = TAG_EDIT_LOOKUP_TIMEOUT,
            helpRes = R.string.svc_help_timeout,
            modifier = Modifier
        )
    }
}
