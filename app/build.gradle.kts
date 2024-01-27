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

    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.dagger)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.github.oheger.wificontrol"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.github.oheger.wificontrol"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.7"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
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

dependencies {
    implementation(libs.accompanist.webview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material)
    implementation(libs.hilt.android)

    kapt(libs.hilt.compiler)

    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.androidx.test.espresso)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.junitVintage)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotest.runner.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

kapt {
    correctErrorTypes = true
}

kotlin {
    // This is needed to avoid an error due to incompatible Java target versions between the
    // 'compileReleaseJavaWithJavac' and 'kaptGenerateStubsReleaseKotlin' tasks; the latter uses as target the
    // local JDK used to execute the Gradle build.
    jvmToolchain(8)
}

// Java 17 is required for Robolectric tests.
tasks.withType<Test>().configureEach {
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
