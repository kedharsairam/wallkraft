package com.wallkraft.app.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wallkraft.app.core.design.KraftTheme
import com.wallkraft.app.core.utils.Result
import com.wallkraft.app.data.prefs.SearchHistoryRepository
import com.wallkraft.app.domain.model.AppSettings
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.model.WallpaperMeta
import com.wallkraft.app.domain.model.WallpaperResponse
import com.wallkraft.app.domain.repository.SettingsRepository
import com.wallkraft.app.domain.repository.WallpaperRepository
import com.wallkraft.app.presentation.browse.BrowseScreen
import com.wallkraft.app.presentation.browse.BrowseViewModel
import com.wallkraft.app.presentation.common.ConnectivityViewModel
import com.wallkraft.app.util.ConnectivityObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for BrowseScreen — no Hilt, ViewModels constructed manually
 * with fakes (screens accept ViewModels as params, HistoryScreen pattern).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@RunWith(AndroidJUnit4::class)
class BrowseScreenSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    private class FakeWallpaperRepository : WallpaperRepository {
        override suspend fun search(
            filters: WallhavenFilters,
            page: Int,
            forceRefresh: Boolean,
        ): Result<WallpaperResponse> = Result.Success(
            WallpaperResponse(
                data = listOf(Wallpaper(id = "wp-1", dimensionX = 1920, dimensionY = 1080)),
                meta = WallpaperMeta(currentPage = 1, lastPage = 1),
            ),
        )
        override suspend fun wallpaper(id: String): Result<Wallpaper> =
            Result.Success(Wallpaper(id = id, dimensionX = 1920, dimensionY = 1080))
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

    private class FakeSearchHistoryStore : SearchHistoryRepository {
        override val history: Flow<List<String>> = MutableStateFlow(emptyList())
        override suspend fun current(): List<String> = emptyList()
        override suspend fun add(query: String) {}
        override suspend fun clear() {}
    }

    private fun testViewModel() = BrowseViewModel(
        repository = FakeWallpaperRepository(),
        settingsRepository = FakeSettingsRepository(),
        searchHistoryStore = FakeSearchHistoryStore(),
        errorMessage = { "Error" },
    )

    private fun testConnectivityViewModel(): ConnectivityViewModel {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return ConnectivityViewModel(ConnectivityObserver(context))
    }

    @Test
    fun search_bar_is_displayed() {
        compose.setContent {
            KraftTheme {
                BrowseScreen(
                    onOpenWallpaper = {},
                    viewModel = testViewModel(),
                    connectivityViewModel = testConnectivityViewModel(),
                )
            }
        }

        compose.onNodeWithText("Search").assertIsDisplayed()
    }

    @Test
    fun filter_button_is_displayed() {
        compose.setContent {
            KraftTheme {
                BrowseScreen(
                    onOpenWallpaper = {},
                    viewModel = testViewModel(),
                    connectivityViewModel = testConnectivityViewModel(),
                )
            }
        }

        compose.onNodeWithContentDescription("Filters").assertIsDisplayed()
    }

    @Test
    fun loading_state_shows_shimmer_grid() {
        compose.setContent {
            KraftTheme {
                BrowseScreen(
                    onOpenWallpaper = {},
                    viewModel = testViewModel(),
                    connectivityViewModel = testConnectivityViewModel(),
                )
            }
        }

        compose.onNodeWithContentDescription("Loading wallpapers").assertIsDisplayed()
    }
}
