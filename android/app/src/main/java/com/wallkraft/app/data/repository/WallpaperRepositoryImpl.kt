package com.wallkraft.app.data.repository

import android.util.LruCache
import com.wallkraft.app.core.utils.Result
import com.wallkraft.app.data.api.WallhavenApi
import com.wallkraft.app.data.cache.SearchResponseCache
import com.wallkraft.app.domain.model.Purity
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.model.WallpaperResponse
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
    ): Result<WallpaperResponse> {
        // 1. Fresh cache hit → instant, no network round-trip. Returning to
        //    a screen you already visited is immediate. Skipped on a forced
        //    refresh, which must return live data from the server.
        if (!forceRefresh && searchCache.isFresh(filters, page)) {
            searchCache.get(filters, page)?.let { return Result.Success(it) }
        }

        // 2. Network fetch via WallhavenApi which already maps exceptions to AppError.
        val apiResult = api.search(filters, page)
        return when (apiResult) {
            is Result.Success -> {
                val response = apiResult.data
                // Defense in depth: only surface purities the user asked for.
                // Also dedupe by id — the Wallhaven search API can return the
                // same wallpaper twice across pages.
                val allowed = buildSet {
                    if (Purity.SFW in filters.purity) add("sfw")
                    if (Purity.Sketchy in filters.purity) add("sketchy")
                    if (Purity.NSFW in filters.purity) add("nsfw")
                }
                val filtered = response.data
                    .filter { it.purity in allowed }
                    .distinctBy { it.id }
                val processed = WallpaperResponse(
                    data = filtered,
                    // Preserve the server total (e.g. 104,367). filtered.size is
                    // just the page size (24) — the API already filters purity
                    // server-side, this client filter is defense-in-depth only.
                    // Overwriting total with page size is what showed "24".
                    meta = response.meta,
                )
                searchCache.put(filters, page, processed)
                Result.Success(processed)
            }
            is Result.Failure -> {
                // 3. Offline fallback — a stale cached copy beats an error.
                //    Skipped on a forced refresh too: pulling to refresh with no
                //    connection should surface the error, not silently replay old
                //    results.
                if (!forceRefresh) {
                    searchCache.get(filters, page)?.let { return Result.Success(it) }
                }
                apiResult
            }
        }
    }

    override suspend fun wallpaper(id: String): Result<Wallpaper> =
        cacheById.get(id)?.let { Result.Success(it) }
            ?: when (val result = api.wallpaper(id)) {
                is Result.Success -> {
                    cacheById.put(id, result.data)
                    result
                }
                is Result.Failure -> result
            }

    override fun observeRateLimited(): Flow<Boolean> = api.observeRateLimited()
}
