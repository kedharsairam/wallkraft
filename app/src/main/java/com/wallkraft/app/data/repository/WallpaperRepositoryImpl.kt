package com.wallkraft.app.data.repository

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

    private val cacheById = mutableMapOf<String, Wallpaper>()
    private val snapshot = mutableListOf<Wallpaper>()

    override suspend fun search(filters: WallhavenFilters, page: Int): Result<WallpaperResponse> =
        try {
            val response = api.search(filters, page)
            // Defense in depth: even though the API client only ever requests
            // SFW (purity=100), never surface anything the API reports as
            // sketchy or NSFW. Also dedupe by id — the Wallhaven search API
            // can return the same wallpaper twice across pages, and the
            // staggered grid keys items by id.
            val sfwOnly = response.data
                .filter { it.isSfw }
                .distinctBy { it.id }
            snapshot.clear()
            snapshot.addAll(sfwOnly)
            sfwOnly.forEach { cacheById[it.id] = it }
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
        cacheById[id]?.let { Result.success(it) }
            ?: try {
                Result.success(api.wallpaper(id).also { cacheById[id] = it })
            } catch (e: WallpaperError) {
                Result.failure(e)
            }

    override fun cached(id: String): Wallpaper? = cacheById[id]

    override fun cacheSnapshot(): List<Wallpaper> = snapshot.toList()

    override fun observeRateLimited(): Flow<Boolean> = api.observeRateLimited()

    override fun rateLimitRemaining(): Int = api.rateLimitRemaining()
}
