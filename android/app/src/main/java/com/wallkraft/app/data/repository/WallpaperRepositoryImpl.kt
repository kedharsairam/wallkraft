package com.wallkraft.app.data.repository

import android.util.LruCache
import com.wallkraft.app.data.api.WallhavenApi
import com.wallkraft.app.data.cache.SearchResponseCache
import com.wallkraft.app.domain.model.Purity
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.model.WallpaperResponse
import com.wallkraft.app.domain.repository.WallpaperError
import com.wallkraft.app.domain.repository.WallpaperRepository
import kotlinx.coroutines.flow.Flow

class WallpaperRepositoryImpl(
    private val api: WallhavenApi,
    private val searchCache: SearchResponseCache,
) : WallpaperRepository {

    /** Bounded in-memory cache — caps at 200 entries to prevent OOM on long sessions. */
    private val cacheById = LruCache<String, Wallpaper>(200)

    override suspend fun search(
        filters: WallhavenFilters,
        page: Int,
        forceRefresh: Boolean,
    ): Result<WallpaperResponse> =
        try {
            // 1. Fresh cache hit → instant, no network round-trip. Returning to
            //    a screen you already visited is immediate. Skipped on a forced
            //    refresh, which must return live data from the server.
            if (!forceRefresh && searchCache.isFresh(filters, page)) {
                searchCache.get(filters, page)?.let { return Result.success(it) }
            }

            // 2. Network fetch.
            val response = api.search(filters, page)
            // Defense in depth: only surface purities the user asked for.
            // SFW (100), Sketchy (010), or both (110) — NSFW (001) is never
            // requested and never shown, even if the API somehow returns it.
            // Also dedupe by id — the Wallhaven search API can return the
            // same wallpaper twice across pages.
            val allowed = buildSet {
                if (Purity.SFW in filters.purity) add("sfw")
                if (Purity.Sketchy in filters.purity) add("sketchy")
            }
            val filtered = response.data
                .filter { it.purity in allowed }
                .distinctBy { it.id }
            val processed = WallpaperResponse(
                data = filtered,
                meta = response.meta.copy(total = filtered.size),
            )
            searchCache.put(filters, page, processed)
            Result.success(processed)
        } catch (e: Exception) {
            // 3. Offline fallback — a stale cached copy beats an error.
            //    Skipped on a forced refresh too: pulling to refresh with no
            //    connection should surface the error, not silently replay old
            //    results.
            if (!forceRefresh) {
                searchCache.get(filters, page)?.let { return Result.success(it) }
            }
            val error = if (e is WallpaperError) e else WallpaperError.Api("Unexpected error: ${e.message}")
            Result.failure(error)
        }

    override suspend fun wallpaper(id: String): Result<Wallpaper> =
        cacheById.get(id)?.let { Result.success(it) }
            ?: try {
                Result.success(api.wallpaper(id).also { cacheById.put(id, it) })
            } catch (e: Exception) {
                val error = if (e is WallpaperError) e else WallpaperError.Api("Unexpected error: ${e.message}")
                Result.failure(error)
            }

    override fun observeRateLimited(): Flow<Boolean> = api.observeRateLimited()
}
