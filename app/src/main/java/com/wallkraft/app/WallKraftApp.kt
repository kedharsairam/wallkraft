package com.wallkraft.app

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import com.wallkraft.app.core.design.KraftTheme
import com.wallkraft.app.domain.model.AppSettings
import com.wallkraft.app.domain.model.ThemeMode
import com.wallkraft.app.presentation.components.GridImageLoader
import com.wallkraft.app.presentation.components.ImageCache

@Composable
fun WallKraftApp(container: AppContainer) {
    // Crossfade every image load (grid tiles, detail, fullscreen) instead of
    // snapping from the placeholder to the loaded bitmap. Configured once on
    // the singleton loader so every AsyncImage gets it for free. The OkHttp
    // network fetcher is still auto-registered via ServiceLoader. Caches are
    // tuned and shared (see ImageCache) so images stay fast and small.
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .memoryCache { ImageCache.memoryCache(context) }
            .diskCache { ImageCache.diskCache(context) }
            .build()
    }

    // The grid uses a dedicated loader WITHOUT crossfade: fading every tile as
    // it scrolls into view adds per-frame compositing work and makes scrolling
    // feel janky. Thumbnails pop in instantly instead. Shared across all cards.
    val appContext = LocalContext.current.applicationContext
    GridImageLoader.init(appContext)

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
