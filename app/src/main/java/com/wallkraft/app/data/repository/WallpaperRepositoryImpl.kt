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
            snapshot.clear()
            snapshot.addAll(response.data)
            response.data.forEach { cacheById[it.id] = it }
            Result.success(response)
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
