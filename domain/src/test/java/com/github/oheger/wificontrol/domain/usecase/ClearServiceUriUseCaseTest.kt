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
package com.github.oheger.wificontrol.domain.usecase

import com.github.oheger.wificontrol.domain.repo.ServiceUriRepository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.result.shouldBeSuccess

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

class ClearServiceUriUseCaseTest : StringSpec({
    "The service should be cleared in the repository" {
        val serviceName = "serviceToClear"
        val repository = mockk<ServiceUriRepository> {
            every { clearService(serviceName) } just runs
        }

        val useCase = ClearServiceUriUseCase(UseCaseConfig(Dispatchers.IO), repository)
        val result = useCase.execute(ClearServiceUriUseCase.Input(serviceName)).first()

        result shouldBeSuccess ClearServiceUriUseCase.Output
        verify {
            repository.clearService(serviceName)
        }
    }
})
