package com.wallkraft.app

import android.app.Application
import android.os.Process
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

@HiltAndroidApp
class WallKraftApplication : Application() {
    @Deprecated("Use Hilt — kept for tests only, lazy to avoid dual graph in production")
    val container by lazy { AppContainer(this) }

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
        // AppContainer is deprecated — production UI uses Hilt directly, no dual graph.
        // container is lazy and only created for deprecated Screen overloads/tests.
        realignRotationSchedule()
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
            }
        }
    }

    private fun installCrashHandler() {
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
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RateLimitStateEntryPoint {
    fun rateLimitState(): com.wallkraft.app.data.api.RateLimitState
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RotationStoreEntryPoint {
    fun rotationStore(): com.wallkraft.app.data.prefs.RotationStore
}
