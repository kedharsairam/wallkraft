package com.wallkraft.app.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import com.wallkraft.app.domain.model.WallpaperPosition
import com.wallkraft.app.util.WallpaperSetter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies a pre-cropped bitmap as the device wallpaper.
 *
 * Thin use-case wrapper around [WallpaperSetter] that provides named
 * convenience methods for each [WallpaperPosition]. The class is injectable
 * and [WallpaperSetter] is replaceable for testing via [Setter] interface.
 */
@Singleton
class SetWallpaperUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val setter: Setter = Setter.Default,
) {

    /**
     * Abstraction over the wallpaper-setting backend, extracted so unit tests
     * can supply a fake without touching Android APIs.
     */
    interface Setter {
        suspend fun set(context: Context, bitmap: Bitmap?, position: WallpaperPosition): Boolean

        /** Production implementation that delegates to [WallpaperSetter]. */
        data object Default : Setter {
            override suspend fun set(context: Context, bitmap: Bitmap?, position: WallpaperPosition): Boolean =
                bitmap?.let { WallpaperSetter.setAsWallpaper(context, it, position) } ?: false
        }
    }

    /** Applies [bitmap] as the home screen wallpaper. */
    suspend fun setHome(bitmap: Bitmap?): Boolean =
        setter.set(context, bitmap, WallpaperPosition.HOME)

    /** Applies [bitmap] as the lock screen wallpaper. */
    suspend fun setLock(bitmap: Bitmap?): Boolean =
        setter.set(context, bitmap, WallpaperPosition.LOCK)

    /** Applies [bitmap] as both home and lock screen wallpaper. */
    suspend fun setBoth(bitmap: Bitmap?): Boolean =
        setter.set(context, bitmap, WallpaperPosition.BOTH)

    /** Applies [bitmap] at the given [position]. */
    suspend fun set(bitmap: Bitmap?, position: WallpaperPosition): Boolean =
        setter.set(context, bitmap, position)
}
