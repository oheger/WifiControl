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
plugins {
    kotlin("kapt")

    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt.dagger)
    alias(libs.plugins.kotlin.android)
}

val defaultCompileSdk: String by project
val defaultMinSdk: String by project
val defaultJvmTarget: String by project
val javaCompileVersion: String by project
val jvmToolChainVersion: String by project

android {
    namespace = "com.github.oheger.wificontrol.domain"
    compileSdk = defaultCompileSdk.toInt()

    defaultConfig {
        minSdk = defaultMinSdk.toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(javaCompileVersion)
        targetCompatibility = JavaVersion.toVersion(javaCompileVersion)
    }
    kotlinOptions {
        jvmTarget = defaultJvmTarget
    }
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    // This is needed to avoid an error due to incompatible Java target versions between the
    // 'compileReleaseJavaWithJavac' and 'kaptGenerateStubsReleaseKotlin' tasks; the latter uses as target the
    // local JDK used to execute the Gradle build.
    jvmToolchain(jvmToolChainVersion.toInt())
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)

    kapt(libs.hilt.compiler)

    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotest.runner.junit)
    testImplementation(libs.mockk)
}
