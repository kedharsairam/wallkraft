package com.wallkraft.app.data.repository

import com.wallkraft.app.core.errors.AppError
import com.wallkraft.app.core.utils.Result
import com.wallkraft.app.data.api.WallhavenApiSource
import com.wallkraft.app.data.cache.SearchResponseCache
import com.wallkraft.app.domain.model.Category
import com.wallkraft.app.domain.model.Orientation
import com.wallkraft.app.domain.model.Purity
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.model.WallpaperMeta
import com.wallkraft.app.domain.model.WallpaperResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Offline-mode behavior for [WallpaperRepositoryImpl].
 *
 * Verifies the repository's offline fallback: empty-cache → empty/error,
 * stale cache → stale data, offline→online transitions, rate-limit
 * during offline, and detail-view fallback.
 */
class OfflineBehaviorTest {

    private lateinit var fakeApi: FakeApi
    private lateinit var fakeCache: FakeCache
    private lateinit var repo: WallpaperRepositoryImpl

    @Before
    fun setUp() {
        fakeApi = FakeApi()
        fakeCache = FakeCache()
        repo = WallpaperRepositoryImpl(fakeApi, fakeCache)
    }

    private fun filters(query: String = "") = WallhavenFilters(
        categories = setOf(Category.General),
        sorting = Sorting.DateAdded,
        orientation = Orientation.Both,
        query = query,
        purity = setOf(Purity.SFW),
    )

    private fun response(ids: List<String>, total: Int = ids.size) = WallpaperResponse(
        data = ids.map { Wallpaper(id = it, path = "https://example.com/$it.jpg") },
        meta = WallpaperMeta(currentPage = 1, lastPage = 1, total = total),
    )

    // --- search offline behavior ---

    @Test
    fun `search offline with empty cache returns empty`() = runTest {
        val f = filters()
        fakeApi.searchResult = Result.Failure(AppError.NetworkError.NoConnection)
        fakeCache.cachedResponse = null

        val result = repo.search(f, page = 1)

        assertTrue(result is Result.Failure)
    }

    @Test
    fun `search offline with cached data returns stale data`() = runTest {
        val f = filters()
        fakeApi.searchResult = Result.Failure(AppError.NetworkError.NoConnection)
        fakeCache.cachedResponse = response(listOf("cached1", "cached2"))

        val result = repo.search(f, page = 1)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(2, data.data.size)
        assertEquals("cached1", data.data[0].id)
    }

    @Test
    fun `search offline then online replaces stale with fresh data`() = runTest {
        val f = filters()
        // First call: offline, cache is stale
        fakeApi.searchResult = Result.Failure(AppError.NetworkError.NoConnection)
        fakeCache.cachedResponse = response(listOf("stale1"))
        val offlineResult = repo.search(f, page = 1)
        assertTrue(offlineResult is Result.Success)
        assertEquals("stale1", (offlineResult as Result.Success).data.data[0].id)

        // Second call: back online, cache is no longer fresh so API is called
        fakeCache.isFreshResult = false
        fakeApi.searchResult = Result.Success(response(listOf("fresh1", "fresh2")))
        val onlineResult = repo.search(f, page = 1)

        assertTrue(onlineResult is Result.Success)
        val data = (onlineResult as Result.Success).data
        assertEquals(2, data.data.size)
        assertEquals("fresh1", data.data[0].id)
    }

    @Test
    fun `search offline with rateLimited returns cached data`() = runTest {
        val f = filters()
        // API returns RateLimited — the repository still falls back to the
        // stale cache when the request is not a forced refresh.
        fakeApi.searchResult = Result.Failure(AppError.NetworkError.RateLimited())
        fakeCache.cachedResponse = response(listOf("cached1"))

        val result = repo.search(f, page = 1)

        assertTrue(result is Result.Success)
        assertEquals("cached1", (result as Result.Success).data.data[0].id)
    }

    // --- wallpaper() offline behavior ---

    @Test
    fun `wallpaper offline with cached metadata returns cached wallpaper`() = runTest {
        val wp = Wallpaper(id = "wp1", path = "https://example.com/wp1.jpg", views = 42)
        fakeCache.isWallpaperFreshResult = true
        fakeCache.cachedWallpaper = wp
        fakeApi.wallpaperResult = Result.Failure(AppError.NetworkError.NoConnection)

        val result = repo.wallpaper("wp1")

        assertTrue(result is Result.Success)
        assertEquals(42, (result as Result.Success).data.views)
    }

    @Test
    fun `wallpaper offline without cache returns error`() = runTest {
        fakeApi.wallpaperResult = Result.Failure(AppError.NetworkError.NoConnection)
        fakeCache.cachedWallpaper = null

        val result = repo.wallpaper("missing")

        assertTrue(result is Result.Failure)
    }

    @Test
    fun `wallpaper offline stale disk still returns cached data`() = runTest {
        val wp = Wallpaper(id = "wp2", path = "https://example.com/wp2.jpg", views = 77)
        fakeCache.isWallpaperFreshResult = false // expired
        fakeCache.cachedWallpaper = wp
        fakeApi.wallpaperResult = Result.Failure(AppError.NetworkError.NoConnection)

        val result = repo.wallpaper("wp2")

        assertTrue(result is Result.Success)
        assertEquals(77, (result as Result.Success).data.views)
    }

    // --- Fakes ---

    private class FakeApi : WallhavenApiSource {
        var searchResult: Result<WallpaperResponse> = Result.Success(WallpaperResponse())
        var wallpaperResult: Result<Wallpaper> = Result.Failure(AppError.DataError.NotFound)

        override suspend fun search(filters: WallhavenFilters, page: Int): Result<WallpaperResponse> =
            searchResult

        override suspend fun wallpaper(id: String): Result<Wallpaper> = wallpaperResult
        override suspend fun validateApiKey(key: String): Boolean = true
        override fun observeRateLimited(): Flow<Boolean> = flowOf(false)
    }

    private class FakeCache : SearchResponseCache(
        directory = java.io.File(System.getProperty("java.io.tmpdir"), "offline_test_${System.nanoTime()}"),
        json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true },
    ) {
        var isFreshResult = false
        var cachedResponse: WallpaperResponse? = null
        var isWallpaperFreshResult = false
        var cachedWallpaper: Wallpaper? = null

        override fun isFresh(filters: WallhavenFilters, page: Int): Boolean = isFreshResult
        override suspend fun get(filters: WallhavenFilters, page: Int): WallpaperResponse? = cachedResponse
        override suspend fun put(filters: WallhavenFilters, page: Int, response: WallpaperResponse): kotlin.Result<Unit> = kotlin.runCatching {}
        override fun isWallpaperFresh(id: String): Boolean = isWallpaperFreshResult
        override suspend fun getWallpaper(id: String): Wallpaper? = cachedWallpaper
        override suspend fun putWallpaper(wallpaper: Wallpaper): kotlin.Result<Unit> = kotlin.runCatching {}
    }
}
