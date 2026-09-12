package com.wallkraft.app.domain.repository

import com.wallkraft.app.core.utils.Result
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.model.WallpaperResponse
import kotlinx.coroutines.flow.Flow

/** Legacy error type — kept for reference but no longer thrown. Prefer [com.wallkraft.app.core.errors.AppError]. */
@Deprecated("Use AppError via core.utils.Result", ReplaceWith("com.wallkraft.app.core.errors.AppError"))
sealed class WallpaperError : Exception() {
    data object RateLimited : WallpaperError() {
        override val message: String = "Rate limited"
    }
    data class Api(override val message: String, val code: Int? = null) : WallpaperError()
}

/**
 * Fetches wallpapers from the Wallhaven API.
 *
 * [search] pages through results; [wallpaper] fetches a single wallpaper by id.
 * Errors are surfaced as [com.wallkraft.app.core.errors.AppError] via [Result.Failure].
 *
 * [search] serves fresh results from the response cache when available. Pass
 * `forceRefresh = true` to bypass the cache and hit the network — used by
 * pull-to-refresh, which must return live data (and needs the network latency
 * so the refresh indicator animates away properly).
 */
interface WallpaperRepository {
    suspend fun search(
        filters: WallhavenFilters,
        page: Int,
        forceRefresh: Boolean = false,
    ): Result<WallpaperResponse>
    suspend fun wallpaper(id: String): Result<Wallpaper>
    fun observeRateLimited(): Flow<Boolean>
}
