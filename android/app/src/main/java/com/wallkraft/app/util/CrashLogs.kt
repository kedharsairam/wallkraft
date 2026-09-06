package com.wallkraft.app.util

import android.content.Context
import java.io.File

/**
 * Reads crash logs written by the uncaught-exception handler
 * (see WallKraftApplication).
 *
 * Logs never leave the device on their own — this only locates the newest
 * one so the user can explicitly share it from Settings → About.
 */
object CrashLogs {

    /** The newest non-empty crash log, or null when there are none. */
    fun latestCrashLog(context: Context): File? =
        latestIn(File(context.cacheDir, "crash"))

    /** Newest non-empty `crash-*.log` in [dir], or null. Pure logic, unit-tested. */
    fun latestIn(dir: File): File? =
        dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("crash-") && it.length() > 0 }
            ?.maxByOrNull { it.lastModified() }
}
