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
