package com.wallkraft.app

import android.app.Application
import android.os.Process
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

@HiltAndroidApp
class WallKraftApplication : Application() {
    /** Application-scoped coroutine scope for non-UI work (e.g. RateLimit cooldown). */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        installCrashHandler()
        // RateLimitState is now a Hilt singleton; bind the app scope via EntryPoint
        // to avoid field injection on Application (which triggers Kotlin metadata
        // version issues with Hilt 2.52 + Kotlin 2.1.0).
        runCatching {
            val entryPoint = EntryPointAccessors.fromApplication(
                this,
                RateLimitStateEntryPoint::class.java,
            )
            entryPoint.rateLimitState().attachScope(applicationScope)
        }
        // GridImageLoader now has Hilt singleton support but keep static init for
        // transition — its companion will delegate to the Hilt instance when available
        // and create a fallback otherwise (so startup before Hilt injection still works).
        com.wallkraft.app.core.cache.GridImageLoader.init(this)
        realignRotationSchedule()
        observeReconnectAndRepair()
        pruneOldHistory()
    }

    /**
     * One-shot upgrade: pre-v2 periodic work ran on rolling timers ("24h from
     * tap") and survives app updates inside WorkManager. Re-enqueue active
     * schedules with boundary alignment, then never again.
     */
    /**
     * Rotation chain reconciliation, every cold start. A force-stop (user,
     * task killer, or my own adb testing) drops scheduled jobs WITHOUT
     * clearing our flags — a one-shot migration flag would leave the schedule
     * silently dead. So instead: kill legacy rolling work (idempotent),
     * then if a schedule is active but no live chain link exists, enqueue a
     * fresh boundary-aligned link. Healthy starts are a cheap no-op query.
     */
    private fun realignRotationSchedule() {
        applicationScope.launch {
            runCatching {
                val manager = androidx.work.WorkManager.getInstance(this@WallKraftApplication)
                manager.cancelUniqueWork(com.wallkraft.app.data.rotation.RotationScheduler.UNIQUE_PERIODIC)
                val rotationEntryPoint = EntryPointAccessors.fromApplication(
                    this@WallKraftApplication,
                    RotationStoreEntryPoint::class.java,
                )
                val schedule = rotationEntryPoint.rotationStore().current().schedule
                if (schedule == com.wallkraft.app.domain.model.RotationSchedule.OFF) return@launch
                val live = manager.getWorkInfosForUniqueWorkFlow(com.wallkraft.app.data.rotation.RotationScheduler.UNIQUE_CHAIN)
                    .first()
                    .any {
                        it.state == androidx.work.WorkInfo.State.ENQUEUED ||
                            it.state == androidx.work.WorkInfo.State.RUNNING ||
                            it.state == androidx.work.WorkInfo.State.BLOCKED
                    }
                if (!live) {
                    com.wallkraft.app.data.rotation.RotationScheduler.apply(this@WallKraftApplication, schedule)
                }
            }.onFailure { e ->
                if (BuildConfig.DEBUG) {
                    Log.e("WallKraftApplication", "Rotation schedule reconciliation failed", e)
                }
            }
        }
    }

    private fun installCrashHandler() {
        // Age-based expiry: crash logs older than 14 days never accumulate.
        // (Count is bounded to the newest 3 by the handler below.)
        runCatching { com.wallkraft.app.util.CrashLogs.pruneOld(this) }
        val default = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val dir = File(cacheDir, "crash").apply { mkdirs() }
                // Keep only last 3 crash logs to bound storage.
                dir.listFiles()?.sortedBy { it.lastModified() }
                    ?.dropLast(2)?.forEach { it.delete() }
                val file = File(dir, "crash-${System.currentTimeMillis()}.log")
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                file.writeText(
                    "Thread: ${thread.name} pid=${Process.myPid()}\n" +
                        "Exception: ${throwable::class.java.name}: ${throwable.message}\n" +
                        sw.toString(),
                )
            } catch (_: Exception) {
                // Never crash the crash handler.
            } finally {
                default?.uncaughtException(thread, throwable)
                    ?: run { Process.killProcess(Process.myPid()); System.exit(2) }
            }
        }
    }

    /**
     * Mid-session recovery: when connectivity drops and returns, re-run the
     * offline repair for favorites missing a local copy (favorites minus
     * [com.wallkraft.app.data.cache.OfflineImageStore.fileFor] present).
     *
     * The screen-entry trigger in FavoritesScreen stays as-is (belt and
     * suspenders): it covers cold starts with missing files, this covers
     * mid-session recovery. No schema change — the missing set is derived
     * at runtime.
     *
     * Debounce: rapid flaps are ignored — a reconnect only repairs when the
     * device was offline for at least 5s, and repairs are spaced at least
     * 10s apart.
     */
    private fun observeReconnectAndRepair() {
        applicationScope.launch {
            runCatching {
                val entryPoint = EntryPointAccessors.fromApplication(
                    this@WallKraftApplication,
                    RepairOnReconnectEntryPoint::class.java,
                )
                val connectivity = entryPoint.connectivityObserver()
                val favoritesRepository = entryPoint.favoritesRepository()
                val imageStore = entryPoint.favoriteImageStore()
                val repair = com.wallkraft.app.data.cache.FavoriteOfflineRepair(imageStore)
                var wentOfflineAt = -1L
                var lastRepairAt = 0L
                var previous = true
                connectivity.isOnline.collect { online ->
                    val now = System.currentTimeMillis()
                    if (!online) {
                        if (previous) wentOfflineAt = now
                    } else if (!previous) {
                        val offlineFor = now - wentOfflineAt
                        val sinceLastRepair = now - lastRepairAt
                        if (wentOfflineAt > 0 &&
                            offlineFor >= RECONNECT_MIN_OFFLINE_MS &&
                            sinceLastRepair >= RECONNECT_MIN_REPAIR_GAP_MS
                        ) {
                            lastRepairAt = now
                            val wallpapers = withContext(Dispatchers.IO) {
                                favoritesRepository.observeWallpapers().first()
                            }
                            val missing = withContext(Dispatchers.IO) {
                                repair.missing(wallpapers)
                            }
                            if (missing.isNotEmpty()) {
                                withContext(Dispatchers.IO) {
                                    repair.repairAll(missing)
                                }
                            }
                        }
                    }
                    previous = online
                }
            }.onFailure { e ->
                if (BuildConfig.DEBUG) {
                    Log.e("WallKraftApplication", "Reconnect repair failed", e)
                }
            }
        }
    }

    companion object {
        /** Reconnect only counts when offline for at least this long (flap filter). */
        const val RECONNECT_MIN_OFFLINE_MS = 5_000L

        /** Minimum gap between two reconnect repairs. */
        const val RECONNECT_MIN_REPAIR_GAP_MS = 10_000L

        /** Keep wallpaper history for 90 days. */
        const val HISTORY_RETENTION_MS = 90L * 24 * 60 * 60 * 1000
    }

    private fun pruneOldHistory() {
        applicationScope.launch {
            runCatching {
                val entryPoint = EntryPointAccessors.fromApplication(
                    this@WallKraftApplication,
                    HistoryPruneEntryPoint::class.java,
                )
                val cutoff = System.currentTimeMillis() - HISTORY_RETENTION_MS
                withContext(Dispatchers.IO) {
                    entryPoint.wallpaperHistoryDao().deleteOlderThan(cutoff)
                }
            }.onFailure { e ->
                if (BuildConfig.DEBUG) {
                    Log.e("WallKraftApplication", "History prune failed", e)
                }
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RateLimitStateEntryPoint {
    fun rateLimitState(): com.wallkraft.app.data.api.RateLimitState
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RotationStoreEntryPoint {
    fun rotationStore(): com.wallkraft.app.data.prefs.RotationSettingsStore
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RepairOnReconnectEntryPoint {
    fun connectivityObserver(): com.wallkraft.app.util.ConnectivityObserver
    fun favoritesRepository(): com.wallkraft.app.domain.repository.FavoritesRepository
    fun favoriteImageStore(): com.wallkraft.app.data.cache.OfflineImageStore
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HistoryPruneEntryPoint {
    fun wallpaperHistoryDao(): com.wallkraft.app.data.db.WallpaperHistoryDao
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerEntryPoint {
    fun rotationStore(): com.wallkraft.app.data.prefs.RotationSettingsStore
    fun favoritesRepository(): com.wallkraft.app.domain.repository.FavoritesRepository
    fun collectionsRepository(): com.wallkraft.app.domain.repository.CollectionsRepository
    fun favoriteImageStore(): com.wallkraft.app.data.cache.OfflineImageStore
    fun rotationCropStore(): com.wallkraft.app.data.prefs.CropStore
    fun wallpaperHistoryDao(): com.wallkraft.app.data.db.WallpaperHistoryDao
}
