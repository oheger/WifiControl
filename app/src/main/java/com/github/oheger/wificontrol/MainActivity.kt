package com.github.oheger.wificontrol

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier

import com.github.oheger.wificontrol.ui.theme.WifiControlTheme

import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity() {
    companion object {
        /**
         * The configuration for searching the server in the network. This is currently hard-coded (which will change
         * later).
         */
        private val serverFinderConfig = ServerFinderConfig(
            multicastAddress = "231.10.0.0",
            port = 4321,
            requestCode = "playerServer?",
            networkTimeout = 30.seconds,
            retryDelay = 60.seconds
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val model = ControlViewModelImpl()
        val controller = ServerLookupController.create(model, this, serverFinderConfig)
        controller.startLookup()

        setContent {
            WifiControlTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    ControlUi(model)
                }
            }
        }
    }
}
