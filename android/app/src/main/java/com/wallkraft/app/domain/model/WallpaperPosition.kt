package com.wallkraft.app.domain.model

import android.app.WallpaperManager

/** Which screen(s) to apply a wallpaper to. */
enum class WallpaperPosition(val flags: Int) {
    HOME(WallpaperManager.FLAG_SYSTEM),
    LOCK(WallpaperManager.FLAG_LOCK),
    BOTH(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK),
}
