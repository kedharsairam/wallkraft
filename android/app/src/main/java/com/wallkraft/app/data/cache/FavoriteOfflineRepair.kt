package com.wallkraft.app.data.cache

import com.wallkraft.app.domain.model.Wallpaper

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
     * Runs on the caller's thread (suspend); offload with Dispatchers.IO.
     */
    suspend fun repairAll(
        wallpapers: List<Wallpaper>,
        onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> },
    ): RepairResult {
        var restored = 0
        val failed = mutableListOf<String>()
        wallpapers.forEachIndexed { index, wallpaper ->
            if (store.save(wallpaper)) restored++ else failed += wallpaper.id
            onProgress(index + 1, wallpapers.size)
        }
        return RepairResult(restored, failed)
    }
}
