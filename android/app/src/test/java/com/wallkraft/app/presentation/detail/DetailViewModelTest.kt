package com.wallkraft.app.presentation.detail

import com.wallkraft.app.core.errors.AppError
import com.wallkraft.app.core.utils.Result
import com.wallkraft.app.data.cache.OfflineImageStore
import com.wallkraft.app.data.prefs.CropStore
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.model.Favorite
import com.wallkraft.app.domain.model.CropRect
import com.wallkraft.app.domain.repository.FavoritesRepository
import com.wallkraft.app.domain.repository.SettingsRepository
import com.wallkraft.app.domain.repository.WallpaperRepository
import com.wallkraft.app.domain.model.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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
import java.io.File

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
            Result.Success(Wallpaper(id = id, dimensionX = 2560, dimensionY = 1440))
        }
        val vm = DetailViewModel("wp-1", repo, FakeFavoritesRepository(), FakeSettingsRepository(), FakeFavoriteImageStore(), FakeRotationCropStore(), errorMessage = { "error" })
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
        repo.wallpaperResult = { Result.Failure(AppError.Unknown(message = "network")) }
        val vm = DetailViewModel("wp-1", repo, FakeFavoritesRepository(), FakeSettingsRepository(), FakeFavoriteImageStore(), FakeRotationCropStore(), errorMessage = { e -> (e as? AppError.Unknown)?.message ?: "unknown" })
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
            Result.Success(Wallpaper(id = id, dimensionX = 1920, dimensionY = 1080))
        }
        val vm = DetailViewModel("wp-1", repo, favRepo, FakeSettingsRepository(), FakeFavoriteImageStore(), FakeRotationCropStore(), errorMessage = { "error" })
        advanceUntilIdle()

        assertFalse("wp-1" in vm.uiState.value.favoriteIds)
        vm.toggleFavorite(Wallpaper(id = "wp-1", dimensionX = 1920, dimensionY = 1080))
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
            Result.Success(Wallpaper(id = id, dimensionX = 1920, dimensionY = 1080))
        }
        val vm = DetailViewModel("wp-1", repo, favRepo, FakeSettingsRepository(), FakeFavoriteImageStore(), FakeRotationCropStore(), errorMessage = { "error" })
        advanceUntilIdle()

        vm.toggleFavorite(Wallpaper(id = "wp-1", dimensionX = 1920, dimensionY = 1080))
        advanceUntilIdle()

        assertFalse(favRepo.isFavorite("wp-1"))
    }

    @Test
    fun `reload cancels previous load`() = runTest(dispatcher) {
        val repo = FakeWallpaperRepository()
        var callCount = 0
        repo.wallpaperResult = { id ->
            callCount++
            Result.Success(Wallpaper(id = "$id-$callCount", dimensionX = 1920, dimensionY = 1080))
        }
        val vm = DetailViewModel("wp-1", repo, FakeFavoritesRepository(), FakeSettingsRepository(), FakeFavoriteImageStore(), FakeRotationCropStore(), errorMessage = { "error" })
        advanceUntilIdle()

        vm.load()
        advanceUntilIdle()

        // Should have been called twice (init + reload)
        assertEquals(2, callCount)
    }

    @Test
    fun `initial state is loading`() = runTest(dispatcher) {
        val repo = FakeWallpaperRepository()
        val vm = DetailViewModel("wp-1", repo, FakeFavoritesRepository(), FakeSettingsRepository(), FakeFavoriteImageStore(), FakeRotationCropStore(), errorMessage = { "error" })

        // Before advancing, should be in loading state
        assertTrue(vm.uiState.value.isLoading)
    }

    @Test
    fun `isFavorite reflects repository state`() = runTest(dispatcher) {
        val favRepo = FakeFavoritesRepository()
        favRepo.addedIds.add("wp-1")
        favRepo.emitFavorites()
        val repo = FakeWallpaperRepository()
        repo.wallpaperResult = { id ->
            Result.Success(Wallpaper(id = id, dimensionX = 1920, dimensionY = 1080))
        }
        val vm = DetailViewModel("wp-1", repo, favRepo, FakeSettingsRepository(), FakeFavoriteImageStore(), FakeRotationCropStore(), errorMessage = { "error" })
        advanceUntilIdle()

        assertTrue("wp-1" in vm.uiState.value.favoriteIds)
    }

    @Test
    fun `load failure does not set wallpaper`() = runTest(dispatcher) {
        val repo = FakeWallpaperRepository()
        repo.wallpaperResult = { Result.Failure(AppError.Unknown(message = "not found")) }
        val vm = DetailViewModel("wp-1", repo, FakeFavoritesRepository(), FakeSettingsRepository(), FakeFavoriteImageStore(), FakeRotationCropStore(), errorMessage = { e -> (e as? AppError.Unknown)?.message ?: "error" })
        advanceUntilIdle()

        assertNull(vm.uiState.value.wallpaper)
        assertEquals("not found", vm.uiState.value.error)
    }

    @Test
    fun `isDetailLoaded becomes true after a successful load`() = runTest(dispatcher) {
        val repo = FakeWallpaperRepository()
        repo.wallpaperResult = { id -> Result.Success(Wallpaper(id = id)) }
        val vm = DetailViewModel("wp-1", repo, FakeFavoritesRepository(), FakeSettingsRepository(), FakeFavoriteImageStore(), FakeRotationCropStore(), errorMessage = { "error" })
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isDetailLoaded)
    }

    @Test
    fun `isDetailLoaded stays false when the load fails`() = runTest(dispatcher) {
        val repo = FakeWallpaperRepository()
        repo.wallpaperResult = { Result.Failure(AppError.Unknown(message = "network")) }
        val vm = DetailViewModel("wp-1", repo, FakeFavoritesRepository(), FakeSettingsRepository(), FakeFavoriteImageStore(), FakeRotationCropStore(), errorMessage = { "error" })
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isDetailLoaded)
    }

    @Test
    fun `preview seed keeps isDetailLoaded false`() = runTest(dispatcher) {
        val repo = FakeWallpaperRepository()
        repo.wallpaperResult = { Result.Success(Wallpaper(id = it)) }
        val vm = DetailViewModel(
            "wp-1", repo, FakeFavoritesRepository(), FakeSettingsRepository(), FakeFavoriteImageStore(), FakeRotationCropStore(), errorMessage = { "error" },
            previewThumb = "thumb.jpg", previewPath = "path.jpg",
        )

        // Before the network resolves, the preview exists but detail is not loaded.
        assertTrue(vm.uiState.value.wallpaper != null)
        assertFalse(vm.uiState.value.isDetailLoaded)
    }

    @Test
    fun `disk fallback success renders wallpaper without error`() = runTest(dispatcher) {
        // Simulates the repository's disk fallback: offline, but a cached
        // copy of the full metadata is returned as Success.
        val repo = FakeWallpaperRepository()
        repo.wallpaperResult = { id ->
            Result.Success(Wallpaper(id = id, path = "https://example.com/$id.jpg", views = 100))
        }
        val vm = DetailViewModel("wp-1", repo, FakeFavoritesRepository(), FakeSettingsRepository(), FakeFavoriteImageStore(), FakeRotationCropStore(), errorMessage = { "error" })
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(100, state.wallpaper?.views)
        assertTrue(state.isDetailLoaded)
    }

    @Test
    fun `offline failure with preview keeps preview and hides error`() = runTest(dispatcher) {
        val repo = FakeWallpaperRepository()
        repo.wallpaperResult = { Result.Failure(AppError.NetworkError.NoConnection) }
        val vm = DetailViewModel(
            "wp-1", repo, FakeFavoritesRepository(), FakeSettingsRepository(), FakeFavoriteImageStore(), FakeRotationCropStore(), errorMessage = { "offline" },
            previewThumb = "thumb.jpg", previewPath = "path.jpg",
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        // The preview covers the screen, so no error is surfaced.
        assertNull(state.error)
        assertEquals("path.jpg", state.wallpaper?.path)
        assertFalse(state.isDetailLoaded)
    }

    @Test
    fun `retry after offline failure can succeed`() = runTest(dispatcher) {
        val repo = FakeWallpaperRepository()
        var online = false
        repo.wallpaperResult = { id ->
            if (online) Result.Success(Wallpaper(id = id)) else Result.Failure(AppError.NetworkError.NoConnection)
        }
        val vm = DetailViewModel("wp-1", repo, FakeFavoritesRepository(), FakeSettingsRepository(), FakeFavoriteImageStore(), FakeRotationCropStore(), errorMessage = { "offline" })
        advanceUntilIdle()
        assertEquals("offline", vm.uiState.value.error)

        online = true
        vm.load()
        advanceUntilIdle()

        assertNull(vm.uiState.value.error)
        assertEquals("wp-1", vm.uiState.value.wallpaper?.id)
        assertTrue(vm.uiState.value.isDetailLoaded)
    }

    private class FakeWallpaperRepository : WallpaperRepository {
        var wallpaperResult: (String) -> Result<Wallpaper> = {
            Result.Success(Wallpaper(id = it, dimensionX = 1920, dimensionY = 1080))
        }

        override suspend fun search(
            filters: com.wallkraft.app.domain.model.WallhavenFilters,
            page: Int,
            forceRefresh: Boolean,
        ): Result<com.wallkraft.app.domain.model.WallpaperResponse> =
            Result.Success(com.wallkraft.app.domain.model.WallpaperResponse())

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
        override fun observeWallpapers(): Flow<List<Wallpaper>> = _favorites.map { favs -> favs.map { it.wallpaper } }
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

    private class FakeSettingsRepository : SettingsRepository {
        private val _settings = MutableStateFlow(AppSettings())
        override val settings: Flow<AppSettings> = _settings
        override suspend fun current(): AppSettings = _settings.value
        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            _settings.value = transform(_settings.value)
        }
    }

    private class FakeFavoriteImageStore : OfflineImageStore {
        private val files = mutableMapOf<String, File>()
        override fun fileFor(id: String): File? = files[id]
        override suspend fun save(wallpaper: Wallpaper): Boolean {
            files[wallpaper.id] = File("/fake/${wallpaper.id}")
            return true
        }
        override fun delete(id: String) { files.remove(id) }
    }

    private class FakeRotationCropStore : CropStore {
        private val _crops = MutableStateFlow<Map<String, CropRect>>(emptyMap())
        override val crops: Flow<Map<String, CropRect>> = _crops
        override suspend fun current(): Map<String, CropRect> = _crops.value
        override suspend fun save(id: String, rect: CropRect) {
            _crops.value = _crops.value + (id to rect)
        }
    }
}
