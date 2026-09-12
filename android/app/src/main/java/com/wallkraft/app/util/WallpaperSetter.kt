package com.wallkraft.app.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import com.wallkraft.app.domain.model.WallpaperPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Maps a pure [WallpaperPosition] to the corresponding [WallpaperManager] flags. */
fun WallpaperPosition.toFlags(): Int = when (this) {
    WallpaperPosition.HOME -> WallpaperManager.FLAG_SYSTEM
    WallpaperPosition.LOCK -> WallpaperManager.FLAG_LOCK
    WallpaperPosition.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
}

/**
 * Applies a pre-cropped bitmap as the device wallpaper.
 *
 * Split from the former WallpaperActions god object — this file owns
 * wallpaper setting and nothing else.
 */
object WallpaperSetter {

    /** Applies a pre-cropped [Bitmap] as the wallpaper at [position]. Off main thread. */
    suspend fun setAsWallpaper(context: Context, bitmap: Bitmap, position: WallpaperPosition): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val wm = WallpaperManager.getInstance(context)
                wm.setBitmap(bitmap, null, true, position.toFlags())
            }.isSuccess
        }
}
