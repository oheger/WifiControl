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

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.oheger.wificontrol.ui.theme.WifiControlTheme

internal const val TAG_SEARCHING_FOR_SERVER = "tag_searching_for_server"
internal const val TAG_SEARCHING_HINT = "tag_searching_for_server_hint"
internal const val TAG_SEARCHING_INDICATOR = "tag_searching_indicator"
internal const val TAG_SERVER_NOT_FOUND = "tag_server_not_found"
internal const val TAG_SERVER_NOT_FOUND_HINT = "tag_server_not_found_hint"
internal const val TAG_WIFI_AVAILABLE = "tag_wifi_available"
internal const val TAG_WIFI_UNAVAILABLE = "tag_wifi_unavailable"
internal const val TAG_WIFI_UNAVAILABLE_HINT = "tag_wifi_unavailable_hint"
internal const val TAG_WIFI_UNKNOWN = "tag_wifi_unknown"

/**
 * Generate the tag for a text line in the notification.
 */
internal fun textTag(tag: String): String = "${tag}_text"

/**
 * Generate the tag for an icon in the notification.
 */
internal fun iconTag(tag: String): String = "${tag}_icon"

@Composable
fun ControlUi(lookupState: ServerLookupState, modifier: Modifier = Modifier) {
    when (lookupState) {
        is ServerFound -> Text("Server found.")

        is NetworkStatusUnknown -> Notification(R.drawable.ic_unknown, TAG_WIFI_UNKNOWN, modifier) {
            NotificationText(R.string.state_wifi_unknown, TAG_WIFI_UNKNOWN, modifier)
        }

        is WiFiUnavailable ->
            Notification(R.drawable.ic_wifi_unavailable, TAG_WIFI_UNAVAILABLE, modifier) {
                Column(modifier = modifier.fillMaxWidth()) {
                    NotificationText(R.string.state_wifi_unavailable, TAG_WIFI_UNAVAILABLE, modifier)
                    Text(
                        stringResource(R.string.state_wifi_unavailable_hint),
                        modifier = modifier.testTag(TAG_WIFI_UNAVAILABLE_HINT)
                    )
                }
            }

        is SearchingInWiFi ->
            Column(modifier = modifier.fillMaxWidth()) {
                WiFiAvailable(modifier)
                Notification(R.drawable.ic_search_server, TAG_SEARCHING_FOR_SERVER, modifier) {
                    Column(modifier = modifier.fillMaxWidth()) {
                        NotificationText(R.string.state_searching, TAG_SEARCHING_FOR_SERVER, modifier)
                        Text(
                            stringResource(R.string.state_searching_hint),
                            modifier = modifier.testTag(TAG_SEARCHING_HINT)
                        )
                        ProgressIndicator(modifier)
                    }
                }
            }

        is ServerNotFound ->
            Column(modifier = modifier.fillMaxWidth()) {
                WiFiAvailable(modifier)
                Notification(R.drawable.ic_server, TAG_SERVER_NOT_FOUND, modifier) {
                    Column(modifier = modifier.fillMaxWidth()) {
                        NotificationText(R.string.state_server_not_found, TAG_SERVER_NOT_FOUND, modifier)
                        Text(
                            stringResource(R.string.state_server_not_found_hint),
                            modifier = modifier.testTag(TAG_SERVER_NOT_FOUND_HINT)
                        )
                    }
                }
            }
    }
}

/**
 * Generate a notification indicating the availability of the Wi-Fi.
 */
@Composable
fun WiFiAvailable(modifier: Modifier) {
    Notification(R.drawable.ic_wifi_available, TAG_WIFI_AVAILABLE, modifier) {
        NotificationText(R.string.state_wifi_available, TAG_WIFI_AVAILABLE, modifier)
    }
}

/**
 * Generate a row with a notification about the progress of the lookup operation. A notification consists of an
 * [icon][iconRes] and other [content] that is aligned right to the icon. The icon is assigned the given [tag].
 */
@Composable
fun Notification(
    iconRes: Int,
    tag: String,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(5.dp)
    ) {
        Icon(painter = painterResource(iconRes), contentDescription = null, modifier.testTag(iconTag(tag)))
        Spacer(modifier.width(width = 4.dp))
        content()
    }
}

/**
 * Generate the text of a notification from the given [resource ID][textRes]. Set the given [tag].
 */
@Composable
fun NotificationText(textRes: Int, tag: String, modifier: Modifier) {
    Text(
        stringResource(textRes),
        fontSize = 18.sp,
        modifier = modifier.testTag(textTag(tag))
    )
}

/**
 * Generate an infinite progress indicator. This is used to show that currently a check for the server in the network
 * is ongoing.
 */
@Composable
fun ProgressIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val progressAnimationValue by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(animation = tween(900))
    )
    CircularProgressIndicator(
        progress = progressAnimationValue,
        modifier = modifier
            .size(16.dp)
            .testTag(TAG_SEARCHING_INDICATOR)
    )
}

@Preview
@Composable
fun ControlUiPreview(
    @PreviewParameter(LookupStatePreviewProvider::class)
    lookupState: ServerLookupState
) {
    WifiControlTheme {
        ControlUi(lookupState)
    }
}

/**
 * A [PreviewParameterProvider] implementation to generate previews for all server lookup states.
 */
class LookupStatePreviewProvider : PreviewParameterProvider<ServerLookupState> {
    override val values: Sequence<ServerLookupState> = sequenceOf(
        NetworkStatusUnknown,
        WiFiUnavailable,
        ServerNotFound,
        SearchingInWiFi,
        ServerFound("http://www.example.org/")
    )
}
