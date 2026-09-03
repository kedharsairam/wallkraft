package com.wallkraft.app

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import com.wallkraft.app.core.design.KraftTheme
import com.wallkraft.app.core.cache.GridImageLoader
import com.wallkraft.app.core.cache.ImageCache

@Composable
fun WallKraftApp(container: AppContainer) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .memoryCache { ImageCache.memoryCache(context) }
            .diskCache { ImageCache.diskCache(context) }
            .build()
    }

    val appContext = LocalContext.current.applicationContext
    GridImageLoader.init(appContext)

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
