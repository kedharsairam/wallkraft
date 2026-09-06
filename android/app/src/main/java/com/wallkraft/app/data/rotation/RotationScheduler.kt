package com.wallkraft.app.data.rotation

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.wallkraft.app.domain.model.RotationSchedule
import java.util.concurrent.TimeUnit

/**
 * Schedules / cancels wallpaper rotation. WorkManager persists the schedule
 * across reboots itself — no boot receiver, no extra permissions.
 */
object RotationScheduler {

    const val UNIQUE_PERIODIC = "wallkraft-rotation"
    private const val UNIQUE_ONCE = "wallkraft-rotation-once"

    fun apply(context: Context, schedule: RotationSchedule) {
        val manager = WorkManager.getInstance(context)
        if (schedule == RotationSchedule.OFF) {
            manager.cancelUniqueWork(UNIQUE_PERIODIC)
            return
        }
        val hours = if (schedule == RotationSchedule.DAILY) 24L else 24L * 7
        val request = PeriodicWorkRequestBuilder<RotateWallpaperWorker>(hours, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .build()
        manager.enqueueUniquePeriodicWork(UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /** Applies the next wallpaper right now (same pipeline as scheduled runs). */
    fun rotateNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<RotateWallpaperWorker>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_ONCE, ExistingWorkPolicy.REPLACE, request)
    }
}
