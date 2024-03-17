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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.github.oheger.wificontrol.R
import com.github.oheger.wificontrol.domain.model.PersistentService
import com.github.oheger.wificontrol.domain.model.ServiceDefinition
import com.github.oheger.wificontrol.ui.theme.WifiControlTheme

internal const val TAG_SHOW_NAME = "svcShowName"
internal const val TAG_SHOW_MULTICAST = "svcShowMulticast"
internal const val TAG_SHOW_PORT = "svcShowPort"
internal const val TAG_SHOW_CODE = "svcShowCode"

/** The indent of the property value relative to the associated label. */
internal const val PROPERTY_INDENT = 10

/**
 * Generate the screen showing the details of a specific service, which also allows editing the service. Use the given
 * [viewModel] to load and access the data to be displayed. Process the service identified by the given
 * [serviceIndex].
 */
@Composable
fun ServiceDetailsScreen(viewModel: ServiceDetailsViewModel, serviceIndex: Int) {
    viewModel.loadService(serviceIndex)

    viewModel.uiStateFlow.collectAsState(ServicesUiStateLoading).value
        .let { state ->
            ServiceDetailsScreenForState(viewModel = viewModel, state = state)
        }
}

/**
 * Generate the screen for the details of a service based on the given [viewModel] and [state].
 */
@Composable
private fun ServiceDetailsScreenForState(
    viewModel: ServiceDetailsViewModel,
    state: ServicesUiState<ServiceDetailsState>,
    modifier: Modifier = Modifier
) {
    ServicesScreen(state = state) { detailsState ->
        ServiceDetails(service = detailsState.service, modifier = modifier)
    }
}

/**
 * Generate the UI to show the details of the given [service].
 */
@Composable
private fun ServiceDetails(service: PersistentService, modifier: Modifier) {
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
 * Generate the label for a service property. The label text is defined by the given [resource ID][labelRes].
 */
@Composable
private fun PropertyLabel(labelRes: Int, modifier: Modifier) {
    Text(text = stringResource(id = labelRes), fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = modifier)
}

@Preview
@Composable
fun ServiceDetailsPreview() {
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
    val model = PreviewServiceDetailsViewModel(service)
    val state = ServicesUiStateLoaded(ServiceDetailsState(0, service))

    WifiControlTheme {
        ServiceDetailsScreenForState(viewModel = model, state = state)
    }
}
