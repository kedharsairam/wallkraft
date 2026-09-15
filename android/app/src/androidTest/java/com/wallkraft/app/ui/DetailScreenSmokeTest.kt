package com.wallkraft.app.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wallkraft.app.core.design.KraftTheme
import com.wallkraft.app.core.utils.Result
import com.wallkraft.app.data.cache.OfflineImageStore
import com.wallkraft.app.data.prefs.CropStore
import com.wallkraft.app.data.db.WallpaperHistoryDao
import com.wallkraft.app.data.db.WallpaperHistoryEntity
import com.wallkraft.app.domain.model.CropRect
import com.wallkraft.app.domain.model.AppSettings
import com.wallkraft.app.domain.model.Favorite
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.model.WallpaperResponse
import com.wallkraft.app.domain.repository.FavoritesRepository
import com.wallkraft.app.domain.repository.SettingsRepository
import com.wallkraft.app.domain.repository.WallpaperRepository
import com.wallkraft.app.presentation.common.ConnectivityViewModel
import com.wallkraft.app.presentation.detail.DetailScreen
import com.wallkraft.app.presentation.detail.DetailViewModel
import com.wallkraft.app.util.ConnectivityObserver
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for DetailScreen — no Hilt, ViewModel constructed manually.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@RunWith(AndroidJUnit4::class)
class DetailScreenSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    private class FakeWallpaperRepository : WallpaperRepository {
        override suspend fun search(
            filters: WallhavenFilters,
            page: Int,
            forceRefresh: Boolean,
        ): Result<WallpaperResponse> = Result.Success(WallpaperResponse())
        override suspend fun wallpaper(id: String): Result<Wallpaper> =
            Result.Success(
                Wallpaper(
                    id = id,
                    dimensionX = 1920,
                    dimensionY = 1080,
                    path = "https://example.com/$id.jpg",
                ),
            )
        override fun observeRateLimited(): Flow<Boolean> = MutableStateFlow(false)
    }

    private class FakeFavoritesRepository : FavoritesRepository {
        override fun observeAll(): Flow<List<Favorite>> = MutableStateFlow(emptyList())
        override fun observeWallpapers(): Flow<List<Wallpaper>> = MutableStateFlow(emptyList())
        override suspend fun isFavorite(id: String): Boolean = false
        override suspend fun add(wallpaper: Wallpaper) {}
        override suspend fun remove(id: String) {}
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
        override fun fileFor(id: String): File? = null
        override suspend fun save(wallpaper: Wallpaper): Boolean = true
        override fun delete(id: String) {}
    }

    private class FakeRotationCropStore : CropStore {
        override val crops: Flow<Map<String, CropRect>> = MutableStateFlow(emptyMap())
        override suspend fun current(): Map<String, CropRect> = emptyMap()
        override suspend fun save(id: String, rect: CropRect) {}
    }

    private class FakeWallpaperHistoryDao : WallpaperHistoryDao {
        override fun observeAll(): Flow<List<WallpaperHistoryEntity>> = MutableStateFlow(emptyList())
        override suspend fun insert(entity: WallpaperHistoryEntity) {}
        override suspend fun deleteOlderThan(cutoffMillis: Long) {}
        override suspend fun deleteById(wallpaperId: String, setAt: Long) {}
        override suspend fun count(): Int = 0
    }

    private fun testViewModel() = DetailViewModel(
        id = "test-id",
        wallpaperRepository = FakeWallpaperRepository(),
        favoritesRepository = FakeFavoritesRepository(),
        settingsRepository = FakeSettingsRepository(),
        favoriteImageStore = FakeFavoriteImageStore(),
        rotationCropStore = FakeRotationCropStore(),
        wallpaperHistoryDao = FakeWallpaperHistoryDao(),
        errorMessage = { "Error" },
    )

    private fun testConnectivityViewModel(): ConnectivityViewModel {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return ConnectivityViewModel(ConnectivityObserver(context))
    }

    @Test
    fun back_button_is_displayed() {
        compose.setContent {
            KraftTheme {
                DetailScreen(
                    wallpaperId = "test-id",
                    onBack = {},
                    viewModel = testViewModel(),
                    connectivityViewModel = testConnectivityViewModel(),
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun favorite_button_is_displayed() {
        compose.setContent {
            KraftTheme {
                DetailScreen(
                    wallpaperId = "test-id",
                    onBack = {},
                    viewModel = testViewModel(),
                    connectivityViewModel = testConnectivityViewModel(),
                )
            }
        }
        compose.waitForIdle()

        // Favorite toggle shows "Add to favorites" when not favorited.
        compose.onNodeWithContentDescription("Add to favorites").assertIsDisplayed()
    }
}
