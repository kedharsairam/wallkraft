package com.wallkraft.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as WallKraftApplication).container

        // Dark mode always — paint window background black before content draws.
        window.decorView.setBackgroundColor(Color.BLACK)

        setContent {
            WallKraftApp(container)
        }
    }
}
