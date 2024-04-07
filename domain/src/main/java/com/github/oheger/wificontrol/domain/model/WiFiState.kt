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
package com.github.oheger.wificontrol.domain.model

/**
 * An enumeration class defining constants for the state of the Wi-Fi.
 *
 * Controlling services is possible only if the app is connected to the Wi-Fi. This class defines the connection
 * states relevant in this context. They are used to monitor the Wi-Fi connection permanently and to react on
 * changes.
 */
enum class WiFiState {
    WI_FI_UNAVAILABLE,

    WI_FI_AVAILABLE
}
