package com.wallkraft.app.data.rotation

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.wallkraft.app.domain.model.RotationSchedule
import java.time.Duration
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Schedules / cancels wallpaper rotation. WorkManager persists the schedule
 * across reboots itself — no boot receiver, no extra permissions.
 *
 * No periodic work by design: periodic first-runs fire ASAP (never on a
 * boundary) and flex minimums can't pin hourly runs to :00. Instead every
 * run is a one-time link chained to the NEXT boundary — self-correcting
 * drift, reboot-safe, exactly-once via unique REPLACE. The chain reads the
 * live schedule at each link, so OFF self-terminates and retaps re-anchor.
 */
object RotationScheduler {

    private const val TAG = "WallKraftPerf"

    /** Legacy rolling periodic work (pre-v2) — cancelled, never recreated. */
    internal const val UNIQUE_PERIODIC = "wallkraft-rotation"

    /** The self-perpetuating schedule chain (one link at a time). */
    internal const val UNIQUE_CHAIN = "wallkraft-rotation-chain"

    private const val UNIQUE_ONCE = "wallkraft-rotation-once"

    /** Input flag: chain links perpetuate the schedule; manual runs don't. */
    internal const val KEY_CHAIN = "chain"

    fun apply(context: Context, schedule: RotationSchedule) {
        val manager = WorkManager.getInstance(context)
        manager.cancelUniqueWork(UNIQUE_CHAIN)
        if (schedule == RotationSchedule.OFF) {
            manager.cancelUniqueWork(UNIQUE_PERIODIC)
            return
        }
        // First link waits for the next :00 / midnight / Monday 00:00 — the
        // old rolling timer ("24h from tap") is gone, mornings look fresh.
        // Enabling exactly on a boundary yields delay 0: instant preview.
        val delayMs = RotationTiming.initialDelayMs(
            schedule, System.currentTimeMillis(), ZoneId.systemDefault(),
        )
        enqueueChain(manager, schedule, delayMs)
        if (com.wallkraft.app.BuildConfig.DEBUG) {
            android.util.Log.d(TAG, "rotation $schedule chained, first in ${delayMs / 60_000}min")
        }
    }

    /** Enqueue the next chain link. Called by apply() and by each chain run. */
    fun chainNext(context: Context, schedule: RotationSchedule) {
        if (schedule == RotationSchedule.OFF) return
        val delayMs = RotationTiming.initialDelayMs(
            schedule, System.currentTimeMillis(), ZoneId.systemDefault(),
        )
        enqueueChain(WorkManager.getInstance(context), schedule, delayMs)
    }

    private fun enqueueChain(manager: WorkManager, schedule: RotationSchedule, delayMs: Long) {
        val request = OneTimeWorkRequestBuilder<RotateWallpaperWorker>()
            .setInitialDelay(Duration.ofMillis(delayMs))
            .setInputData(workDataOf(KEY_CHAIN to true))
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .build()
        // REPLACE collapses racers (retap vs in-flight link) into one link.
        // The schedule itself travels via the store, read live at each run.
        manager.enqueueUniqueWork(UNIQUE_CHAIN, ExistingWorkPolicy.REPLACE, request)
    }

    /** Applies the next wallpaper right now (same pipeline as scheduled runs). Returns the request id for result observation. */
    fun rotateNow(context: Context): UUID {
        val request = OneTimeWorkRequestBuilder<RotateWallpaperWorker>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_ONCE, ExistingWorkPolicy.REPLACE, request)
        return request.id
    }

    /** Work states for the manual rotate-now request (for spinner → check/fail UI). */
    fun observeRotateNow(context: Context): Flow<List<WorkInfo>> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(UNIQUE_ONCE)
}
