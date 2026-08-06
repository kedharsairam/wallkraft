package com.wallkraft.app.data.repository

import android.util.LruCache
import com.wallkraft.app.data.api.WallhavenApi
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.model.WallpaperResponse
import com.wallkraft.app.domain.repository.WallpaperError
import com.wallkraft.app.domain.repository.WallpaperRepository
import kotlinx.coroutines.flow.Flow

class WallpaperRepositoryImpl(
    private val api: WallhavenApi,
) : WallpaperRepository {

    /** Bounded in-memory cache — caps at 200 entries to prevent OOM on long sessions. */
    private val cacheById = LruCache<String, Wallpaper>(200)

    override suspend fun search(filters: WallhavenFilters, page: Int): Result<WallpaperResponse> =
        try {
            val response = api.search(filters, page)
            // Defense in depth: even though the API client only ever requests
            // SFW (purity=100), never surface anything the API reports as
            // sketchy or NSFW. Also dedupe by id — the Wallhaven search API
            // can return the same wallpaper twice across pages, and the
            // staggered grid keys items by id.
            //
            // Search results are NOT cached here: the search endpoint omits
            // tags, and a cached tagless copy would shadow the full wallpaper
            // returned by wallpaper(id), hiding tags on the detail screen.
            val sfwOnly = response.data
                .filter { it.isSfw }
                .distinctBy { it.id }
            Result.success(
                WallpaperResponse(
                    data = sfwOnly,
                    meta = response.meta,
                ),
            )
        } catch (e: WallpaperError) {
            Result.failure(e)
        }

    override suspend fun wallpaper(id: String): Result<Wallpaper> =
        cacheById.get(id)?.let { Result.success(it) }
            ?: try {
                Result.success(api.wallpaper(id).also { cacheById.put(id, it) })
            } catch (e: WallpaperError) {
                Result.failure(e)
            }

    override fun observeRateLimited(): Flow<Boolean> = api.observeRateLimited()
}
