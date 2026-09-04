package com.wallkraft.app

import android.app.Application
import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class WallKraftApplication : Application() {
    lateinit var container: AppContainer
        private set

    /** Application-scoped coroutine scope for non-UI work (e.g. RateLimit cooldown). */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        installCrashHandler()
        com.wallkraft.app.data.api.RateLimitState.attachScope(applicationScope)
        com.wallkraft.app.core.cache.GridImageLoader.init(this)
        container = AppContainer(this)
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
