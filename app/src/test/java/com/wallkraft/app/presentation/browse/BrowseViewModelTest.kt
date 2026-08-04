package com.wallkraft.app.presentation.browse

import com.wallkraft.app.domain.model.AppSettings
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.model.WallpaperMeta
import com.wallkraft.app.domain.model.WallpaperResponse
import com.wallkraft.app.domain.repository.SettingsRepository
import com.wallkraft.app.domain.repository.WallpaperRepository
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `search sends the query to the API`() = runTest(dispatcher) {
        val repo = FakeWallpaperRepository()
        val vm = BrowseViewModel(repo, FakeSettingsRepository()) { "error" }
        advanceUntilIdle()

        vm.search("cats")
        advanceUntilIdle()

        assertEquals("cats", repo.searchRequests.last().first.query)
        assertEquals(2, repo.searchRequests.size)
    }

    @Test
    fun `newer search wins when an older request is still in flight`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val repo = FakeWallpaperRepository()
        repo.onSearch = { filters, _ ->
            calls++
            if (calls == 1) {
                gate.await() // first (init) request hangs
                Result.success(pageOf("stale", filters))
            } else {
                Result.success(pageOf(if (filters.query == "new") "new-result" else "other", filters))
            }
        }
        val vm = BrowseViewModel(repo, FakeSettingsRepository()) { "error" }
        advanceUntilIdle() // init search is now suspended on the gate

        vm.search("new") // cancels the suspended init request, starts a fresh one
        advanceUntilIdle()

        // Let the stale response land. It must NOT overwrite the newer results.
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("new-result"), vm.uiState.value.wallpapers.map { it.id })
        assertEquals(false, vm.uiState.value.isInitialLoading)
    }

    @Test
    fun `search failure sets error message`() = runTest(dispatcher) {
        val repo = FakeWallpaperRepository()
        repo.onSearch = { _, _ -> Result.failure(Exception("network")) }
        val vm = BrowseViewModel(repo, FakeSettingsRepository()) { e -> e.message ?: "unknown" }

        vm.search("cats")
        advanceUntilIdle()

        assertEquals("network", vm.uiState.value.error)
        assertTrue(vm.uiState.value.wallpapers.isEmpty())
    }

    @Test
    fun `loadNextPage appends results`() = runTest(dispatcher) {
        val repo = FakeWallpaperRepository()
        repo.onSearch = { filters, page ->
            Result.success(
                WallpaperResponse(
                    data = listOf(Wallpaper(id = "wp-p$page", dimensionX = 1920, dimensionY = 1080)),
                    meta = WallpaperMeta(currentPage = page, lastPage = 2),
                ),
            )
        }
        val vm = BrowseViewModel(repo, FakeSettingsRepository()) { "error" }
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.wallpapers.size)

        vm.loadNextPage()
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.wallpapers.size)
        assertEquals("wp-p2", vm.uiState.value.wallpapers[1].id)
    }

    @Test
    fun `loadNextPage does nothing when already at last page`() = runTest(dispatcher) {
        val repo = FakeWallpaperRepository()
        repo.onSearch = { filters, page ->
            Result.success(
                WallpaperResponse(
                    data = listOf(Wallpaper(id = "wp-p$page", dimensionX = 1920, dimensionY = 1080)),
                    meta = WallpaperMeta(currentPage = page, lastPage = 1),
                ),
            )
        }
        val vm = BrowseViewModel(repo, FakeSettingsRepository()) { "error" }
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.wallpapers.size)

        vm.loadNextPage()
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.wallpapers.size)
    }

    @Test
    fun `loadNextPage failure does not crash`() = runTest(dispatcher) {
        val repo = FakeWallpaperRepository()
        var callCount = 0
        repo.onSearch = { filters, page ->
            callCount++
            if (callCount == 1) {
                Result.success(
                    WallpaperResponse(
                        data = listOf(Wallpaper(id = "wp-1", dimensionX = 1920, dimensionY = 1080)),
                        meta = WallpaperMeta(currentPage = 1, lastPage = 2),
                    ),
                )
            } else {
                Result.failure(Exception("network"))
            }
        }
        val vm = BrowseViewModel(repo, FakeSettingsRepository()) { e -> e.message ?: "error" }
        advanceUntilIdle()

        vm.loadNextPage()
        advanceUntilIdle()

        // Should not crash, error message should be set
        assertEquals("network", vm.uiState.value.error)
    }

    private fun pageOf(id: String, filters: WallhavenFilters): WallpaperResponse =
        WallpaperResponse(
            data = listOf(Wallpaper(id = id, dimensionX = 1920, dimensionY = 1080)),
            meta = WallpaperMeta(currentPage = 1, lastPage = 1),
        )

    private class FakeWallpaperRepository : WallpaperRepository {
        val searchRequests = mutableListOf<Pair<WallhavenFilters, Int>>()
        var onSearch: suspend (WallhavenFilters, Int) -> Result<WallpaperResponse> = { filters, page ->
            Result.success(
                WallpaperResponse(
                    data = listOf(Wallpaper(id = "wp-$page", dimensionX = 1920, dimensionY = 1080)),
                    meta = WallpaperMeta(currentPage = page, lastPage = 1),
                ),
            )
        }

        override suspend fun search(filters: WallhavenFilters, page: Int): Result<WallpaperResponse> {
            searchRequests += filters to page
            return onSearch(filters, page)
        }

        override suspend fun wallpaper(id: String): Result<Wallpaper> =
            Result.success(Wallpaper(id = id, dimensionX = 1920, dimensionY = 1080))

        override fun observeRateLimited(): Flow<Boolean> = MutableStateFlow(false)
    }

    private class FakeSettingsRepository : SettingsRepository {
        private val _settings = MutableStateFlow(AppSettings())
        override val settings: Flow<AppSettings> = _settings
        override suspend fun current(): AppSettings = _settings.value
        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            _settings.value = transform(_settings.value)
        }
    }
}
