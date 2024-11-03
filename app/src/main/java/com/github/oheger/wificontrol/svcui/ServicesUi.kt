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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.github.oheger.wificontrol.R

internal const val TAG_ERROR_HEADER = "svcErrorHeader"
internal const val TAG_ERROR_MSG = "svcErrorMsg"
internal const val TAG_LOADING_INDICATOR = "svcLoading"
internal const val TAG_SAVE_ERROR = "svcSaveError"
internal const val TAG_SAVE_ERROR_MSG = "svcSaveErrorMsg"

/**
 * A generic function to render a screen that is controlled by the given [state]. The function handles the loading
 * and error state on its own. If actual data is available, it delegates to the [screen] function, which is
 * responsible to display the data correctly.
 */
@Composable
fun <T> ServicesScreen(state: ServicesUiState<T>, modifier: Modifier = Modifier, screen: @Composable (T) -> Unit) {
    when (state) {
        is ServicesUiStateLoading ->
            ServicesLoading(modifier = modifier)

        is ServicesUiStateLoaded ->
            screen(state.data)

        is ServicesUiStateError ->
            ServicesError(exception = state.error, modifier = modifier)
    }
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
            color = MaterialTheme.colors.error,
            fontSize = 30.sp,
            modifier = modifier
                .testTag(TAG_ERROR_HEADER)
                .padding(bottom = 32.dp)
                .align(Alignment.CenterHorizontally)
        )
        Text(
            text = stringResource(id = R.string.svc_load_error_details),
            color = MaterialTheme.colors.error,
            modifier = modifier.padding(bottom = 16.dp)
        )
        Text(
            text = exception.toString(),
            color = MaterialTheme.colors.error,
            fontStyle = FontStyle.Italic,
            modifier = modifier.testTag(TAG_ERROR_MSG)
        )
    }
}

/**
 * Generate a screen with an optional [error] message for a failed save operation. The main part of the screen is
 * defined by the given [screen] function. If [error] is not *null*, display a message with the given [errorHintRes]
 * resource ID and some details about the error.
 */
@Composable
fun ServicesScreenWithSaveError(
    error: Throwable?,
    errorHintRes: Int,
    modifier: Modifier,
    screen: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        screen()

        if(error != null) {
             Spacer(modifier = modifier.weight(1.0f, fill = true))
            Text(
                text = stringResource(id = errorHintRes),
                color = MaterialTheme.colors.error,
                modifier = modifier.testTag(TAG_SAVE_ERROR)
            )
            Text(
                text = error.toString(),
                color = MaterialTheme.colors.error,
                fontStyle = FontStyle.Italic,
                modifier = modifier.testTag(TAG_SAVE_ERROR_MSG)
            )
        }
    }
}
