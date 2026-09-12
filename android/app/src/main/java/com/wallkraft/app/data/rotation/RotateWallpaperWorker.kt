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
        // NOTE: no schedule-OFF early return for manual runs. Periodic work
        // only exists while a schedule is active (apply() cancels it when
        // OFF), so a manual run means an explicit Rotate-now tap — it must run.
        // Chain links are different: the schedule may have been turned OFF
        // while a link was pending, in which case terminate quietly. A live
        // chain perpetuates FIRST so a crash mid-rotation never kills it.
        val settings = container.rotation.current()
        if (inputData.getBoolean(RotationScheduler.KEY_CHAIN, false)) {
            if (settings.schedule == com.wallkraft.app.domain.model.RotationSchedule.OFF) {
                return Result.success()
            }
            RotationScheduler.chainNext(applicationContext, settings.schedule)
        }

        val favorites = container.favoritesRepository.observeAll().first()
            .map { it.wallpaper }
        val pool = if (settings.sourceCollectionId != null) {
            val memberIds = container.collectionsRepository.observeAll().first()
                .firstOrNull { it.id == settings.sourceCollectionId }
                ?.items?.toSet()
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
