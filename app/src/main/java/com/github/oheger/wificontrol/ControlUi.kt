/*
 * Copyright 2023 Oliver Heger.
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

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider

@Composable
fun ControlUi(lookupState: ServerLookupState, modifier: Modifier = Modifier) {
    when(lookupState) {
        is ServerFound -> Text("Server found.")
        else -> Text("Server not found.", modifier = modifier.testTag("foo"))
    }
}

@Preview
@Composable
fun ControlUiPreview(
    @PreviewParameter(LookupStatePreviewProvider::class)
    lookupState: ServerLookupState
) {
    ControlUi(lookupState)
}

class LookupStatePreviewProvider : PreviewParameterProvider<ServerLookupState> {
    override val values: Sequence<ServerLookupState> = sequenceOf(
        NetworkStatusUnknown, WiFiUnavailable, SearchingInWiFi, ServerFound("http://192.168.0.2:8088/")
    )
}
