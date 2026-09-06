package com.wallkraft.app.data.rotation

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wallkraft.app.WallKraftApplication
import com.wallkraft.app.domain.model.RotationTarget
import com.wallkraft.app.domain.model.WallpaperPosition
import com.wallkraft.app.util.RotationFraming
import com.wallkraft.app.util.RotationPicker
import com.wallkraft.app.util.RotationRender
import com.wallkraft.app.util.WallpaperSetter
import kotlinx.coroutines.flow.first
import kotlin.math.min

/**
 * Applies the next favorited wallpaper on schedule.
 *
 * Fully offline: candidates come from local favorite files, framing reuses
 * the user's saved crops (or center-crop / blur modes), and the result is
 * set through the same path as a manual set. Tries up to 3 candidates so
 * one corrupt file never wedges the schedule.
 */
class RotateWallpaperWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as WallKraftApplication).container
        // NOTE: no schedule-OFF early return. Periodic work only exists while
        // a schedule is active (apply() cancels it when OFF), so reaching here
        // means either a live schedule or an explicit Rotate-now tap — both
        // must run.
        val settings = container.rotation.current()

        val favorites = container.favoritesRepository.observeAll().first()
            .map { it.wallpaper }
        val pool = if (settings.sourceCollectionId != null) {
            val memberIds = container.collectionsRepository.observeAll().first()
                .firstOrNull { it.collection.id == settings.sourceCollectionId }
                ?.items?.map { it.wallpaperId }?.toSet()
                ?: emptySet()
            favorites.filter { it.id in memberIds }
        } else {
            favorites
        }
        val candidates = RotationPicker.candidates(pool).filter { wallpaper ->
            container.favoriteImageStore.fileFor(wallpaper.id) != null
        }
        if (candidates.isEmpty()) return Result.success()

        val metrics = applicationContext.resources.displayMetrics
        val screen = RotationRender.Screen(metrics.widthPixels, metrics.heightPixels)
        val crops = container.rotationCrops.current()
        val position = when (settings.target) {
            RotationTarget.HOME -> WallpaperPosition.HOME
            RotationTarget.LOCK -> WallpaperPosition.LOCK
            RotationTarget.BOTH -> WallpaperPosition.BOTH
        }

        var index = RotationPicker.pickNext(candidates.size, settings.lastIndex)
        repeat(min(3, candidates.size)) {
            val wallpaper = candidates[index]
            val file = container.favoriteImageStore.fileFor(wallpaper.id)
            val output = if (file != null) {
                val rect = RotationFraming.frameRect(
                    wallpaper.dimensionX,
                    wallpaper.dimensionY,
                    screen.width,
                    screen.height,
                    crops[wallpaper.id],
                    settings.mode,
                )
                RotationRender.render(file, rect, screen, settings.mode)
            } else {
                null
            }
            if (output != null) {
                try {
                    if (WallpaperSetter.setAsWallpaper(applicationContext, output, position)) {
                        container.rotation.setLastIndex(index)
                        return Result.success()
                    }
                } finally {
                    output.recycle()
                }
            }
            index = (index + 1) % candidates.size
        }
        return Result.failure()
    }
}
