package com.wallkraft.app.data.cache

import com.wallkraft.app.domain.model.Wallpaper
import java.io.File

/**
 * Minimal surface for offline favorite-image storage.
 *
 * Extracted so repair orchestration ([FavoriteOfflineRepair]) can be
 * unit-tested against a fake. [FavoriteImageStore] is the production
 * implementation; behavior is unchanged.
 */
interface OfflineImageStore {
    /** Local file for [id], or null when no valid copy exists. */
    fun fileFor(id: String): File?

    /** Downloads the full-res image for [wallpaper]. True on success. */
    suspend fun save(wallpaper: Wallpaper): Boolean
}
