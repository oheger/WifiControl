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
package com.github.oheger.wificontrol

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput

/**
 * Set the text of this node to the provided [text] making sure that previous text is cleared.
 */
fun SemanticsNodeInteraction.setText(text: String) {
    performScrollTo()
    performTextClearance()
    performTextInput(text)
}

/**
 * Perform a click on this node, making sure that it is visible. Otherwise, clicks on buttons do not have any
 * effect.
 */
fun SemanticsNodeInteraction.performSafeClick() {
    performScrollTo()
    performClick()
}
