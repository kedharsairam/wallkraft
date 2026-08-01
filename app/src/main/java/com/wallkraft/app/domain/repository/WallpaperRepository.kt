package com.wallkraft.app.domain.repository

import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.model.WallpaperResponse
import kotlinx.coroutines.flow.Flow

sealed class WallpaperError : Exception() {
    data object RateLimited : WallpaperError()
    data class Api(override val message: String, val code: Int? = null) : WallpaperError()
}

/**
 * Fetches wallpapers from the Wallhaven API.
 *
 * [search] pages through results; [wallpaper] fetches a single wallpaper by id.
 * Errors are surfaced as [WallpaperError] so callers can show targeted UI
 * (e.g., a rate-limit banner).
 */
interface WallpaperRepository {
    suspend fun search(filters: WallhavenFilters, page: Int): Result<WallpaperResponse>
    suspend fun wallpaper(id: String): Result<Wallpaper>
    fun observeRateLimited(): Flow<Boolean>
}
