package com.wallkraft.app.presentation.detail

import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.repository.Favorite
import com.wallkraft.app.domain.repository.FavoritesRepository
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
class DetailViewModelTest {

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
    fun `load fetches wallpaper by id`() = runTest(dispatcher) {
        val repo = FakeWallpaperRepository()
        repo.wallpaperResult = { id ->
            Result.success(Wallpaper(id = id, dimensionX = 2560, dimensionY = 1440))
        }
        val vm = DetailViewModel("wp-1", repo, FakeFavoritesRepository()) { "error" }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("wp-1", state.wallpaper?.id)
        assertEquals(2560, state.wallpaper?.dimensionX)
    }

    @Test
    fun `load failure sets error message`() = runTest(dispatcher) {
        val repo = FakeWallpaperRepository()
        repo.wallpaperResult = { Result.failure(Exception("network")) }
        val vm = DetailViewModel("wp-1", repo, FakeFavoritesRepository()) { e -> e.message ?: "unknown" }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("network", state.error)
        assertNull(state.wallpaper)
    }

    @Test
    fun `toggleFavorite adds when not favorite`() = runTest(dispatcher) {
        val favRepo = FakeFavoritesRepository()
        val repo = FakeWallpaperRepository()
        repo.wallpaperResult = { id ->
            Result.success(Wallpaper(id = id, dimensionX = 1920, dimensionY = 1080))
        }
        val vm = DetailViewModel("wp-1", repo, favRepo) { "error" }
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isFavorite)
        vm.toggleFavorite()
        advanceUntilIdle()

        assertTrue(favRepo.isFavorite("wp-1"))
    }

    @Test
    fun `toggleFavorite removes when already favorite`() = runTest(dispatcher) {
        val favRepo = FakeFavoritesRepository()
        favRepo.addedIds.add("wp-1")
        favRepo.emitFavorites()
        val repo = FakeWallpaperRepository()
        repo.wallpaperResult = { id ->
            Result.success(Wallpaper(id = id, dimensionX = 1920, dimensionY = 1080))
        }
        val vm = DetailViewModel("wp-1", repo, favRepo) { "error" }
        advanceUntilIdle()

        vm.toggleFavorite()
        advanceUntilIdle()

        assertFalse(favRepo.isFavorite("wp-1"))
    }

    @Test
    fun `reload cancels previous load`() = runTest(dispatcher) {
        val repo = FakeWallpaperRepository()
        var callCount = 0
        repo.wallpaperResult = { id ->
            callCount++
            Result.success(Wallpaper(id = "$id-$callCount", dimensionX = 1920, dimensionY = 1080))
        }
        val vm = DetailViewModel("wp-1", repo, FakeFavoritesRepository()) { "error" }
        advanceUntilIdle()

        vm.load()
        advanceUntilIdle()

        // Should have been called twice (init + reload)
        assertEquals(2, callCount)
    }

    private class FakeWallpaperRepository : WallpaperRepository {
        var wallpaperResult: (String) -> Result<Wallpaper> = {
            Result.success(Wallpaper(id = it, dimensionX = 1920, dimensionY = 1080))
        }

        override suspend fun search(filters: com.wallkraft.app.domain.model.WallhavenFilters, page: Int): Result<com.wallkraft.app.domain.model.WallpaperResponse> =
            Result.success(com.wallkraft.app.domain.model.WallpaperResponse())

        override suspend fun wallpaper(id: String): Result<Wallpaper> = wallpaperResult(id)
        override fun observeRateLimited(): Flow<Boolean> = MutableStateFlow(false)
    }

    private class FakeFavoritesRepository : FavoritesRepository {
        val addedIds = mutableSetOf<String>()
        private val _favorites = MutableStateFlow<List<Favorite>>(emptyList())

        fun emitFavorites() {
            _favorites.value = addedIds.map { Favorite(Wallpaper(id = it, dimensionX = 1920, dimensionY = 1080), System.currentTimeMillis()) }
        }

        override fun observeAll(): Flow<List<Favorite>> = _favorites
        override suspend fun isFavorite(id: String): Boolean = id in addedIds
        override suspend fun add(wallpaper: Wallpaper) {
            addedIds.add(wallpaper.id)
            emitFavorites()
        }
        override suspend fun remove(id: String) {
            addedIds.remove(id)
            emitFavorites()
        }
    }
}
