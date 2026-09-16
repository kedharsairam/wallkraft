/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Kedhar Sairam
 */
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

    /**
     * Deletes `crash-*.log` files in the app crash dir older than [maxAgeDays].
     * Returns the number of files deleted. Never throws.
     */
    fun pruneOld(context: Context, maxAgeDays: Int = 14): Int =
        pruneOld(File(context.cacheDir, "crash"), maxAgeDays)

    /** File overload of [pruneOld] — pure logic, unit-tested. */
    fun pruneOld(dir: File, maxAgeDays: Int = 14, nowMs: Long = System.currentTimeMillis()): Int =
        runCatching {
            val cutoff = nowMs - maxAgeDays * 24 * 60 * 60 * 1000L
            dir.listFiles()
                ?.filter { it.isFile && it.name.startsWith("crash-") && it.lastModified() < cutoff }
                ?.count { it.delete() }
                ?: 0
        }.getOrDefault(0)

    /**
     * Deletes all `crash-*.log` files in the app crash dir.
     * Returns the number of files deleted. Never throws.
     */
    fun deleteAll(context: Context): Int =
        deleteAll(File(context.cacheDir, "crash"))

    /** File overload of [deleteAll] — pure logic, unit-tested. */
    fun deleteAll(dir: File): Int =
        runCatching {
            dir.listFiles()
                ?.filter { it.isFile && it.name.startsWith("crash-") }
                ?.count { it.delete() }
                ?: 0
        }.getOrDefault(0)
}
