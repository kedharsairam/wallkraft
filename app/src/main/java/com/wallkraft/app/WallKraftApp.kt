package com.wallkraft.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    KraftTheme(darkTheme = darkTheme) {
        WallKraftNavHost(container)
    }
}
