package com.wallkraft.app.data.repository

import com.wallkraft.app.core.utils.Result
import com.wallkraft.app.data.api.WallhavenApiSource
import com.wallkraft.app.data.cache.SearchResponseCache
import com.wallkraft.app.domain.model.Purity
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.model.WallpaperResponse
import com.wallkraft.app.domain.repository.WallpaperRepository
import kotlinx.coroutines.flow.Flow

/** Production implementation of [WallpaperRepository] backed by the Wallhaven API and an in-memory cache. */
class WallpaperRepositoryImpl(
    private val api: WallhavenApiSource,
    private val searchCache: SearchResponseCache,
) : WallpaperRepository {

    /** Bounded in-memory cache — caps at 200 entries to prevent OOM on long sessions. */
    private val cacheById = object : LinkedHashMap<String, Wallpaper>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Wallpaper>): Boolean = size > 200
    }

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
                    // Stamp now so the UI gets cachedAt even before the cache
                    // round-trip; put() re-stamps with the same wall-clock.
                    cachedAt = System.currentTimeMillis(),
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

    /**
     * Detail metadata chain: memory LruCache → fresh disk → network.
     * A fresh disk copy (7-day TTL — metadata rarely changes; views and
     * favorites counts go stale gracefully) short-circuits the network so
     * repeat visits are instant and offline-capable. On network failure a
     * stale disk copy still beats an error, mirroring the search fallback.
     */
    override suspend fun wallpaper(id: String): Result<Wallpaper> {
        // 1. Memory hit → instant.
        synchronized(cacheById) { cacheById[id] }?.let { return Result.Success(it) }
        // 2. Fresh disk hit → no network round-trip.
        if (searchCache.isWallpaperFresh(id)) {
            searchCache.getWallpaper(id)?.let {
                synchronized(cacheById) { cacheById[it.id] = it }
                return Result.Success(it)
            }
        }
        // 3. Network fetch, writing through to both caches.
        return when (val result = api.wallpaper(id)) {
            is Result.Success -> {
                synchronized(cacheById) { cacheById[result.data.id] = result.data }
                searchCache.putWallpaper(result.data)
                result
            }
            is Result.Failure -> {
                // 4. Offline fallback — a stale disk copy beats an error.
                searchCache.getWallpaper(id)?.let {
                    synchronized(cacheById) { cacheById[it.id] = it }
                    return Result.Success(it)
                }
                result
            }
        }
    }

    override fun observeRateLimited(): Flow<Boolean> = api.observeRateLimited()
}
