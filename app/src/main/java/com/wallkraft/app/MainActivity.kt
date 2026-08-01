package com.wallkraft.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Show the branded splash before any content renders; must run before
        // super.onCreate for the SplashScreen API to capture the theme attrs.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as WallKraftApplication).container
        setContent {
            WallKraftApp(container)
        }
    }
}
