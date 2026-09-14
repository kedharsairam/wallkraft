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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WallpaperRepositoryImplTest {

    private lateinit var fakeApi: FakeWallhavenApi
    private lateinit var fakeCache: FakeSearchResponseCache
    private lateinit var repo: WallpaperRepositoryImpl

    @Before
    fun setUp() {
        fakeApi = FakeWallhavenApi()
        fakeCache = FakeSearchResponseCache()
        repo = WallpaperRepositoryImpl(fakeApi, fakeCache)
    }

    private fun filters(
        query: String = "",
        purity: Set<Purity> = setOf(Purity.SFW),
    ) = WallhavenFilters(
        categories = setOf(Category.General),
        sorting = Sorting.DateAdded,
        orientation = Orientation.Both,
        query = query,
        purity = purity,
    )

    private fun response(ids: List<String>, total: Int = ids.size) = WallpaperResponse(
        data = ids.map { Wallpaper(id = it, path = "https://example.com/$it.jpg") },
        meta = WallpaperMeta(currentPage = 1, lastPage = 1, total = total),
    )

    // --- search() tests ---

    @Test
    fun `search returns API result and caches on success`() = runTest {
        val f = filters()
        fakeApi.searchResult = Result.Success(response(listOf("1", "2", "3")))

        val result = repo.search(f, page = 1)

        assertTrue(result is Result.Success)
        assertEquals(3, (result as Result.Success).data.data.size)
        assertTrue(fakeCache.wasPut)
    }

    @Test
    fun `search filters out unwanted purities`() = runTest {
        val f = filters(purity = setOf(Purity.SFW))
        fakeApi.searchResult = Result.Success(
            WallpaperResponse(
                data = listOf(
                    Wallpaper(id = "sfw1", path = "a", purity = "sfw"),
                    Wallpaper(id = "nsfw1", path = "b", purity = "nsfw"),
                    Wallpaper(id = "sketchy1", path = "c", purity = "sketchy"),
                ),
                meta = WallpaperMeta(currentPage = 1, lastPage = 1),
            ),
        )

        val result = repo.search(f, page = 1)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data.data
        assertEquals(1, data.size)
        assertEquals("sfw1", data[0].id)
    }

    @Test
    fun `search dedupes wallpapers by id`() = runTest {
        val f = filters()
        fakeApi.searchResult = Result.Success(
            WallpaperResponse(
                data = listOf(
                    Wallpaper(id = "1", path = "a"),
                    Wallpaper(id = "1", path = "b"), // duplicate
                    Wallpaper(id = "2", path = "c"),
                ),
                meta = WallpaperMeta(currentPage = 1, lastPage = 1),
            ),
        )

        val result = repo.search(f, page = 1)

        assertTrue(result is Result.Success)
        assertEquals(2, (result as Result.Success).data.data.size)
    }

    @Test
    fun `search returns stale cache on network failure`() = runTest {
        val f = filters()
        fakeApi.searchResult = Result.Failure(AppError.NetworkError.NoConnection)
        fakeCache.cachedResponse = response(listOf("cached1"))

        val result = repo.search(f, page = 1)

        assertTrue(result is Result.Success)
        assertEquals("cached1", (result as Result.Success).data.data[0].id)
    }

    @Test
    fun `search returns error on network failure when forceRefresh`() = runTest {
        val f = filters()
        fakeApi.searchResult = Result.Failure(AppError.NetworkError.NoConnection)
        fakeCache.cachedResponse = response(listOf("cached1"))

        val result = repo.search(f, page = 1, forceRefresh = true)

        assertTrue(result is Result.Failure)
    }

    @Test
    fun `search skips fresh cache on forceRefresh`() = runTest {
        val f = filters()
        fakeCache.isFreshResult = true
        fakeCache.cachedResponse = response(listOf("cached1"))
        fakeApi.searchResult = Result.Success(response(listOf("fresh1")))

        val result = repo.search(f, page = 1, forceRefresh = true)

        assertTrue(result is Result.Success)
        assertEquals("fresh1", (result as Result.Success).data.data[0].id)
    }

    @Test
    fun `search uses fresh cache without network call`() = runTest {
        val f = filters()
        fakeCache.isFreshResult = true
        fakeCache.cachedResponse = response(listOf("cached1"))

        val result = repo.search(f, page = 1)

        assertTrue(result is Result.Success)
        assertEquals("cached1", (result as Result.Success).data.data[0].id)
        assertEquals(0, fakeApi.searchCallCount) // no network call
    }

    @Test
    fun `search preserves server total count`() = runTest {
        val f = filters()
        fakeApi.searchResult = Result.Success(response(listOf("1", "2"), total = 104367))

        val result = repo.search(f, page = 1)

        assertTrue(result is Result.Success)
        assertEquals(104367, (result as Result.Success).data.meta.total)
    }

    @Test
    fun `search stamps cachedAt on network success so fresh loads are not stale`() = runTest {
        val f = filters()
        val before = System.currentTimeMillis()
        fakeApi.searchResult = Result.Success(response(listOf("1")))

        val result = repo.search(f, page = 1)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertNotNull(data.cachedAt)
        assertTrue(data.cachedAt!! >= before && data.cachedAt!! <= System.currentTimeMillis())
        assertFalse(data.isStale())
    }

    @Test
    fun `search passes stale cachedAt through on offline fallback`() = runTest {
        val f = filters()
        val ttl = 30 * 60 * 1000L
        val now = System.currentTimeMillis()
        fakeApi.searchResult = Result.Failure(AppError.NetworkError.NoConnection)
        fakeCache.cachedResponse = response(listOf("cached1")).copy(cachedAt = now - ttl - 1_000L)

        val result = repo.search(f, page = 1)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(now - ttl - 1_000L, data.cachedAt)
        assertTrue(data.isStale(nowMillis = now, ttlMillis = ttl))
    }

    @Test
    fun `search passes fresh cachedAt through on cache hit`() = runTest {
        val f = filters()
        val ttl = 30 * 60 * 1000L
        val now = System.currentTimeMillis()
        fakeCache.isFreshResult = true
        fakeCache.cachedResponse = response(listOf("cached1")).copy(cachedAt = now - ttl + 60_000L)
        fakeApi.searchResult = Result.Success(response(listOf("fresh1")))

        val result = repo.search(f, page = 1)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals("cached1", data.data[0].id)
        // Within TTL (epsilon inside the boundary) — fresh, never flagged stale.
        assertFalse(data.isStale(nowMillis = now, ttlMillis = ttl))
    }

    // --- wallpaper() tests ---

    @Test
    fun `wallpaper returns from API`() = runTest {
        val wp = Wallpaper(id = "test1", path = "https://example.com/test1.jpg")
        fakeApi.wallpaperResult = Result.Success(wp)

        val result = repo.wallpaper("test1")

        assertTrue(result is Result.Success)
        assertEquals("test1", (result as Result.Success).data.id)
    }

    @Test
    fun `wallpaper returns error from API`() = runTest {
        fakeApi.wallpaperResult = Result.Failure(AppError.DataError.NotFound)

        val result = repo.wallpaper("missing")

        assertTrue(result is Result.Failure)
    }

    @Test
    fun `wallpaper memory hit skips disk and network`() = runTest {
        val wp = Wallpaper(id = "mem1", path = "https://example.com/mem1.jpg")
        fakeApi.wallpaperResult = Result.Success(wp)
        assertTrue(repo.wallpaper("mem1") is Result.Success)

        // Break everything downstream — the memory entry must still win.
        fakeApi.wallpaperResult = Result.Failure(AppError.NetworkError.NoConnection)
        fakeCache.isWallpaperFreshResult = true
        fakeCache.cachedWallpaper = Wallpaper(id = "mem1", path = "https://example.com/stale.jpg")

        val result = repo.wallpaper("mem1")

        assertTrue(result is Result.Success)
        assertEquals("https://example.com/mem1.jpg", (result as Result.Success).data.path)
        assertEquals(1, fakeApi.wallpaperCallCount) // no second network call
        assertEquals(0, fakeCache.wallpaperGetCalls) // no disk read
    }

    @Test
    fun `wallpaper fresh disk hit skips network`() = runTest {
        fakeCache.isWallpaperFreshResult = true
        fakeCache.cachedWallpaper = Wallpaper(id = "disk1", views = 42)
        fakeApi.wallpaperResult = Result.Failure(AppError.NetworkError.NoConnection)

        val result = repo.wallpaper("disk1")

        assertTrue(result is Result.Success)
        assertEquals(42, (result as Result.Success).data.views)
        assertEquals(0, fakeApi.wallpaperCallCount) // offline, no network call
    }

    @Test
    fun `wallpaper network fallback writes through to disk on miss`() = runTest {
        fakeApi.wallpaperResult = Result.Success(Wallpaper(id = "net1", views = 7))

        val result = repo.wallpaper("net1")

        assertTrue(result is Result.Success)
        assertEquals(7, (result as Result.Success).data.views)
        assertEquals(1, fakeApi.wallpaperCallCount)
        assertEquals(1, fakeCache.wallpaperPutCalls)
    }

    @Test
    fun `wallpaper stale disk fallback on network failure`() = runTest {
        fakeApi.wallpaperResult = Result.Failure(AppError.NetworkError.NoConnection)
        fakeCache.isWallpaperFreshResult = false // expired…
        fakeCache.cachedWallpaper = Wallpaper(id = "stale1", views = 99) // …but still usable offline

        val result = repo.wallpaper("stale1")

        assertTrue(result is Result.Success)
        assertEquals(99, (result as Result.Success).data.views)
    }

    @Test
    fun `wallpaper error when memory disk and network all miss`() = runTest {
        fakeApi.wallpaperResult = Result.Failure(AppError.NetworkError.NoConnection)
        fakeCache.cachedWallpaper = null

        val result = repo.wallpaper("missing")

        assertTrue(result is Result.Failure)
    }

    // --- Fakes ---

    class FakeWallhavenApi : WallhavenApiSource {
        var searchResult: Result<WallpaperResponse> = Result.Success(WallpaperResponse(emptyList(), WallpaperMeta(1, 1)))
        var wallpaperResult: Result<Wallpaper> = Result.Failure(AppError.DataError.NotFound)
        var searchCallCount = 0
        var wallpaperCallCount = 0

        override suspend fun search(filters: WallhavenFilters, page: Int): Result<WallpaperResponse> {
            searchCallCount++
            return searchResult
        }

        override suspend fun wallpaper(id: String): Result<Wallpaper> {
            wallpaperCallCount++
            return wallpaperResult
        }

        override suspend fun validateApiKey(key: String): Boolean = true

        override fun observeRateLimited(): Flow<Boolean> = flowOf(false)
    }

    class FakeSearchResponseCache : SearchResponseCache(
        directory = java.io.File(System.getProperty("java.io.tmpdir"), "test_cache_${System.nanoTime()}"),
        json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true },
    ) {
        var isFreshResult = false
        var cachedResponse: WallpaperResponse? = null
        var wasPut = false
        var isWallpaperFreshResult = false
        var cachedWallpaper: Wallpaper? = null
        var wallpaperGetCalls = 0
        var wallpaperPutCalls = 0

        override fun isFresh(filters: WallhavenFilters, page: Int): Boolean = isFreshResult
        override suspend fun get(filters: WallhavenFilters, page: Int): WallpaperResponse? = cachedResponse
        override suspend fun put(filters: WallhavenFilters, page: Int, response: WallpaperResponse): kotlin.Result<Unit> {
            wasPut = true
            return kotlin.Result.success(Unit)
        }
        override fun isWallpaperFresh(id: String): Boolean = isWallpaperFreshResult
        override suspend fun getWallpaper(id: String): Wallpaper? {
            wallpaperGetCalls++
            return cachedWallpaper
        }
        override suspend fun putWallpaper(wallpaper: Wallpaper): kotlin.Result<Unit> {
            wallpaperPutCalls++
            return kotlin.Result.success(Unit)
        }
    }
}
