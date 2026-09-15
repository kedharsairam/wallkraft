package com.wallkraft.app.data.rotation

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wallkraft.app.WorkerEntryPoint
import com.wallkraft.app.data.db.WallpaperHistoryEntity
import com.wallkraft.app.util.RotationEngine
import com.wallkraft.app.util.RotationFraming
import com.wallkraft.app.util.RotationPicker
import com.wallkraft.app.util.RotationRender
import com.wallkraft.app.util.WallpaperSetter
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

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
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            WorkerEntryPoint::class.java,
        )
        val rotationStore = entryPoint.rotationStore()
        val favoritesRepository = entryPoint.favoritesRepository()
        val collectionsRepository = entryPoint.collectionsRepository()
        val favoriteImageStore = entryPoint.favoriteImageStore()
        val rotationCropStore = entryPoint.rotationCropStore()
        val wallpaperHistoryDao = entryPoint.wallpaperHistoryDao()

        // NOTE: no schedule-OFF early return for manual runs. Periodic work
        // only exists while a schedule is active (apply() cancels it when
        // OFF), so a manual run means an explicit Rotate-now tap — it must run.
        // Chain links are different: the schedule may have been turned OFF
        // while a link was pending, in which case terminate quietly. A live
        // chain perpetuates FIRST so a crash mid-rotation never kills it.
        val settings = rotationStore.current()
        val isChain = inputData.getBoolean(RotationScheduler.KEY_CHAIN, false)
        if (!RotationPicker.shouldContinueChain(isChain, settings.schedule)) {
            return Result.success()
        }
        if (isChain) {
            RotationScheduler.chainNext(applicationContext, settings.schedule)
        }

        val favorites = favoritesRepository.observeWallpapers().first()
        val memberIds = if (settings.sourceCollectionId != null) {
            collectionsRepository.observeAll().first()
                .firstOrNull { it.id == settings.sourceCollectionId }
                ?.items?.toSet()
                ?: emptySet()
        } else {
            null
        }
        val pool = RotationPicker.filterByCollection(favorites, memberIds)
        val candidates = RotationPicker.filterAvailable(RotationPicker.candidates(pool)) { id ->
            favoriteImageStore.fileFor(id) != null
        }
        if (candidates.isEmpty()) return Result.failure()

        val metrics = applicationContext.resources.displayMetrics
        val screen = RotationRender.Screen(metrics.widthPixels, metrics.heightPixels)
        val crops = rotationCropStore.current()
        val position = RotationPicker.mapTarget(settings.target)

        val pickResult = RotationEngine.pickNoRepeat(candidates, settings.recentIds)
        if (pickResult.index < 0) return Result.failure()

        val index = pickResult.index
        val wallpaper = candidates[index]
        val file = favoriteImageStore.fileFor(wallpaper.id)
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
                    val window = minOf(candidates.size - 1, 10)
                    rotationStore.setLastIndex(index)
                    rotationStore.appendRecentId(pickResult.id, window)
                    wallpaperHistoryDao.insert(
                        WallpaperHistoryEntity(
                            wallpaperId = wallpaper.id,
                            path = wallpaper.path,
                            thumbnail = wallpaper.thumbnail.orEmpty(),
                            setAt = System.currentTimeMillis(),
                            source = "ROTATION",
                        ),
                    )
                    return Result.success()
                }
            } finally {
                output.recycle()
            }
        }
        return Result.failure()
    }
}
