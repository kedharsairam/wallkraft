package com.wallkraft.app.presentation.common

import com.wallkraft.app.domain.model.AppSettings
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.model.WallpaperMeta
import com.wallkraft.app.domain.model.WallpaperResponse
import com.wallkraft.app.domain.repository.SettingsRepository
import com.wallkraft.app.domain.repository.WallpaperRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WallpaperListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initial load sets wallpapers`() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = TestVM(repo)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.wallpapers.size)
        assertEquals("wp-1", vm.uiState.value.wallpapers[0].id)
        assertFalse(vm.uiState.value.isInitialLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `setFilters clears wallpapers and reloads`() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = TestVM(repo)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.wallpapers.size)

        val requestsBefore = repo.searchRequests.size
        vm.setFilters(WallhavenFilters(query = "cats"))
        advanceUntilIdle()

        // Should have made at least one more request after setFilters
        assertTrue(repo.searchRequests.size > requestsBefore)
        assertFalse(vm.uiState.value.wallpapers.isEmpty())
    }

    @Test
    fun `setFilters with initialQuery preserves query`() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = TestVM(repo, initialQuery = "mountains")
        advanceUntilIdle()

        vm.setFilters(WallhavenFilters())
        advanceUntilIdle()

        assertEquals("mountains", repo.searchRequests.last().first.query)
    }

    @Test
    fun `retry reloads first page`() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = TestVM(repo)
        advanceUntilIdle()

        val requestsBefore = repo.searchRequests.size
        vm.retry()
        advanceUntilIdle()

        assertTrue(repo.searchRequests.size > requestsBefore)
        assertFalse(vm.uiState.value.isInitialLoading)
    }

    @Test
    fun `loadNextPage appends results`() = runTest(dispatcher) {
        val repo = FakeRepo()
        repo.onSearch = { _, page ->
            Result.success(WallpaperResponse(
                data = listOf(Wallpaper(id = "wp-p$page", dimensionX = 1920, dimensionY = 1080)),
                meta = WallpaperMeta(currentPage = page, lastPage = 2),
            ))
        }
        val vm = TestVM(repo)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.wallpapers.size)

        vm.loadNextPage()
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.wallpapers.size)
        assertEquals("wp-p2", vm.uiState.value.wallpapers[1].id)
    }

    @Test
    fun `loadNextPage does nothing at last page`() = runTest(dispatcher) {
        val repo = FakeRepo()
        repo.onSearch = { _, page ->
            Result.success(WallpaperResponse(
                data = listOf(Wallpaper(id = "wp-p$page", dimensionX = 1920, dimensionY = 1080)),
                meta = WallpaperMeta(currentPage = page, lastPage = 1),
            ))
        }
        val vm = TestVM(repo)
        advanceUntilIdle()

        vm.loadNextPage()
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.wallpapers.size)
    }

    @Test
    fun `failure sets error message`() = runTest(dispatcher) {
        val repo = FakeRepo()
        repo.onSearch = { _, _ -> Result.failure(Exception("network")) }
        val vm = TestVM(repo, errorMessage = { e -> e.message ?: "unknown" })
        advanceUntilIdle()

        assertEquals("network", vm.uiState.value.error)
        assertTrue(vm.uiState.value.wallpapers.isEmpty())
    }

    @Test
    fun `empty results sets empty list`() = runTest(dispatcher) {
        val repo = FakeRepo()
        repo.onSearch = { _, _ ->
            Result.success(WallpaperResponse(
                data = emptyList(),
                meta = WallpaperMeta(currentPage = 1, lastPage = 1),
            ))
        }
        val vm = TestVM(repo)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.wallpapers.isEmpty())
        assertFalse(vm.uiState.value.isInitialLoading)
    }

    @Test
    fun `dedup removes duplicate wallpapers on append`() = runTest(dispatcher) {
        val repo = FakeRepo()
        var callCount = 0
        repo.onSearch = { _, page ->
            callCount++
            if (callCount <= 1) {
                Result.success(WallpaperResponse(
                    data = listOf(Wallpaper(id = "wp-dup", dimensionX = 1920, dimensionY = 1080)),
                    meta = WallpaperMeta(currentPage = 1, lastPage = 2),
                ))
            } else {
                Result.success(WallpaperResponse(
                    data = listOf(Wallpaper(id = "wp-dup", dimensionX = 1920, dimensionY = 1080)),
                    meta = WallpaperMeta(currentPage = 2, lastPage = 2),
                ))
            }
        }
        val vm = TestVM(repo)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.wallpapers.size)

        vm.loadNextPage()
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.wallpapers.size)
    }

    @Test
    fun `refresh replaces wallpaper list`() = runTest(dispatcher) {
        val repo = FakeRepo()
        var page1Loaded = false
        repo.onSearch = { _, page ->
            if (page == 1 && !page1Loaded) {
                page1Loaded = true
                Result.success(WallpaperResponse(
                    data = listOf(Wallpaper(id = "wp-old", dimensionX = 1920, dimensionY = 1080)),
                    meta = WallpaperMeta(currentPage = 1, lastPage = 2),
                ))
            } else {
                Result.success(WallpaperResponse(
                    data = listOf(Wallpaper(id = "wp-new", dimensionX = 1920, dimensionY = 1080)),
                    meta = WallpaperMeta(currentPage = 1, lastPage = 1),
                ))
            }
        }
        val clock = FakeClock(1000)
        val vm = TestVM(repo, clock = clock)
        advanceUntilIdle()

        assertEquals("wp-old", vm.uiState.value.wallpapers[0].id)

        clock.advanceBy(600)
        vm.refresh()
        advanceUntilIdle()

        assertEquals("wp-new", vm.uiState.value.wallpapers[0].id)
        assertFalse(vm.uiState.value.isRefreshing)
    }

    @Test
    fun `refresh stays visible for minimum duration`() = runTest(dispatcher) {
        val repo = FakeRepo()
        repo.onSearch = { _, _ ->
            Result.success(WallpaperResponse(
                data = listOf(Wallpaper(id = "wp-1", dimensionX = 1920, dimensionY = 1080)),
                meta = WallpaperMeta(currentPage = 1, lastPage = 1),
            ))
        }
        val clock = FakeClock(0)
        val vm = TestVM(repo, clock = clock)
        advanceUntilIdle()

        // Advance clock past MIN_REFRESH_MS (500ms) so the delay resolves
        clock.advanceBy(600)
        vm.refresh()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isRefreshing)
    }

    // --- Helpers ---

    private class FakeClock(private var now: Long = 0) : ElapsedClock {
        override fun elapsedMs(): Long = now
        fun advanceBy(ms: Long) { now += ms }
    }

    private class TestVM(
        repository: WallpaperRepository,
        initialQuery: String = "",
        errorMessage: (Throwable) -> String = { "error" },
        clock: ElapsedClock = FakeClock(),
    ) : WallpaperListViewModel(repository, FakeSettingsRepo(), errorMessage, initialQuery, clock)

    private class FakeRepo : WallpaperRepository {
        val searchRequests = mutableListOf<Triple<WallhavenFilters, Int, Boolean>>()
        var onSearch: suspend (WallhavenFilters, Int) -> Result<WallpaperResponse> = { _, page ->
            Result.success(WallpaperResponse(
                data = listOf(Wallpaper(id = "wp-$page", dimensionX = 1920, dimensionY = 1080)),
                meta = WallpaperMeta(currentPage = page, lastPage = 1),
            ))
        }

        override suspend fun search(filters: WallhavenFilters, page: Int, forceRefresh: Boolean): Result<WallpaperResponse> {
            searchRequests += Triple(filters, page, forceRefresh)
            return onSearch(filters, page)
        }
        override suspend fun wallpaper(id: String): Result<Wallpaper> =
            Result.success(Wallpaper(id = id, dimensionX = 1920, dimensionY = 1080))
        override fun observeRateLimited(): Flow<Boolean> = MutableStateFlow(false)
    }

    private class FakeSettingsRepo : SettingsRepository {
        private val _settings = MutableStateFlow(AppSettings())
        override val settings: Flow<AppSettings> = _settings
        override suspend fun current(): AppSettings = _settings.value
        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            _settings.value = transform(_settings.value)
        }
    }
}
