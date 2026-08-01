package com.wallkraft.app

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import com.wallkraft.app.core.design.KraftTheme
import com.wallkraft.app.domain.model.AppSettings
import com.wallkraft.app.domain.model.ThemeMode

@Composable
fun WallKraftApp(container: AppContainer) {
    val settings by container.settings.settings.collectAsState(initial = AppSettings())
    val darkTheme = when (settings.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    // Match the system-bar icon color to the resolved theme. `enableEdgeToEdge()`
    // only follows the *system* dark mode, so forcing Light/Dark in Settings left
    // the status/nav icons the wrong color (e.g. dark icons on a black bar).
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowInsetsControllerCompat(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    KraftTheme(darkTheme = darkTheme) {
        WallKraftNavHost(container)
    }
}
