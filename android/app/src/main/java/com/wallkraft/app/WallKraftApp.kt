package com.wallkraft.app

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.wallkraft.app.BuildConfig
import com.wallkraft.app.core.design.KraftTheme
import com.wallkraft.app.core.cache.ImageCache

@Composable
fun WallKraftApp(container: AppContainer) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .memoryCache { ImageCache.memoryCache(context) }
            .diskCache { ImageCache.diskCache(context) }
            // Debug-only: verbose cache hit/miss/decode logging to logcat.
            // Compiled out of release behavior via the DEBUG gate.
            .apply { if (BuildConfig.DEBUG) logger(DebugLogger()) }
            .build()
    }

    // Dark mode always — white status/nav bar icons on black.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowInsetsControllerCompat(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    KraftTheme {
        WallKraftNavHost(container)
    }
}
