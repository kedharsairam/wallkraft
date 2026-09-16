/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Kedhar Sairam
 */
package com.wallkraft.app.data.cache

import com.wallkraft.app.domain.model.Wallpaper
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/**
 * Re-validates and restores offline copies of favorited wallpapers.
 *
 * A favorite whose local file is missing (never downloaded, evicted by the
 * LRU cap, or deleted) is re-downloaded from its Wallhaven URL. When the
 * image is gone server-side too, the restore fails and the id is reported —
 * the metadata stays (the user may still want the entry), the caller decides
 * how to surface it.
 */
class FavoriteOfflineRepair(private val store: OfflineImageStore) {

    data class RepairResult(val restored: Int, val failed: List<String>)

    fun hasLocal(wallpaper: Wallpaper): Boolean =
        store.fileFor(wallpaper.id) != null

    /** Favorites without a valid local copy, in list order (newest first). */
    fun missing(wallpapers: List<Wallpaper>): List<Wallpaper> =
        wallpapers.filter { !hasLocal(it) }

    /**
     * Restores every wallpaper in [wallpapers], reporting progress.
     * Runs up to 4 downloads concurrently for better throughput.
     * Runs on the caller's thread (suspend); offload with Dispatchers.IO.
     */
    suspend fun repairAll(
        wallpapers: List<Wallpaper>,
        onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> },
    ): RepairResult {
        val total = wallpapers.size
        var completed = 0
        var restoredCount = 0
        val failed = java.util.concurrent.ConcurrentLinkedQueue<String>()
        val mutex = Mutex()
        val semaphore = Semaphore(4)
        coroutineScope {
            wallpapers.map { wallpaper ->
                async {
                    semaphore.withPermit {
                        val success = store.save(wallpaper)
                        mutex.withLock {
                            completed++
                            if (success) restoredCount++ else failed.add(wallpaper.id)
                            onProgress(completed, total)
                        }
                    }
                }
            }.forEach { it.await() }
        }
        return RepairResult(restoredCount, failed.toList())
    }
}
