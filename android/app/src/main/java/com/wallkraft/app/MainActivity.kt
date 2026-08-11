package com.wallkraft.app

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.wallkraft.app.domain.model.ThemeMode
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Show the branded splash before any content renders; must run before
        // super.onCreate for the SplashScreen API to capture the theme attrs.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as WallKraftApplication).container

        // The native window theme is always Material.Light, so on its own it
        // flashes the wrong shade (e.g. light on a forced-dark app). Hold the
        // splash until the stored theme is known, then paint the window
        // background to match the resolved theme before content draws.
        var themeResolved = false
        splash.setKeepOnScreenCondition { !themeResolved }
        lifecycleScope.launch {
            try {
                val settings = container.settings.current()
                val dark = when (settings.themeMode) {
                    ThemeMode.System ->
                        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                            Configuration.UI_MODE_NIGHT_YES
                    ThemeMode.Light -> false
                    ThemeMode.Dark -> true
                }
                window.decorView.setBackgroundColor(
                    if (dark) Color.BLACK else Color.rgb(0xF2, 0xF2, 0xF7),
                )
            } finally {
                // Always release the splash, even if reading settings fails.
                themeResolved = true
            }
        }

        setContent {
            WallKraftApp(container)
        }
    }
}