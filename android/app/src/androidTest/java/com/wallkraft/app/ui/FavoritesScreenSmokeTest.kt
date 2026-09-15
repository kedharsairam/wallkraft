package com.wallkraft.app.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wallkraft.app.core.design.KraftTheme
import com.wallkraft.app.data.cache.OfflineImageStore
import com.wallkraft.app.data.prefs.RotationSettings
import com.wallkraft.app.data.prefs.RotationSettingsStore
import com.wallkraft.app.domain.model.RotationMode
import com.wallkraft.app.domain.model.RotationSchedule
import com.wallkraft.app.domain.model.RotationTarget
import com.wallkraft.app.domain.model.AppSettings
import com.wallkraft.app.domain.model.Collection
import com.wallkraft.app.domain.model.Favorite
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.repository.CollectionsRepository
import com.wallkraft.app.domain.repository.FavoritesRepository
import com.wallkraft.app.domain.repository.SettingsRepository
import com.wallkraft.app.presentation.common.ConnectivityViewModel
import com.wallkraft.app.presentation.favorites.CollectionsViewModel
import com.wallkraft.app.presentation.favorites.FavoritesScreen
import com.wallkraft.app.presentation.favorites.FavoritesViewModel
import com.wallkraft.app.util.ConnectivityObserver
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for FavoritesScreen — no Hilt, ViewModels constructed manually.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@RunWith(AndroidJUnit4::class)
class FavoritesScreenSmokeTest {

    @get:Rule
    val compose = createComposeRule()

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

    private class FakeRotationStore : RotationSettingsStore {
        private val _settings = MutableStateFlow(RotationSettings())
        override val settings: Flow<RotationSettings> = _settings
        override val timingWelcomeSeen: Flow<Boolean> = MutableStateFlow(true)
        override suspend fun current() = _settings.value
        override suspend fun setSchedule(schedule: RotationSchedule) {}
        override suspend fun setMode(mode: RotationMode) {}
        override suspend fun setTarget(target: RotationTarget) {}
        override suspend fun setSourceCollection(id: Long?) {}
        override suspend fun setLastIndex(index: Int) {}
        override suspend fun setRecentIds(ids: List<String>) {}
        override suspend fun appendRecentId(id: String, window: Int) {}
        override suspend fun markTimingWelcomeSeen() {}
    }

    private class FakeCollectionsRepository : CollectionsRepository {
        override fun observeAll(): Flow<List<Collection>> = MutableStateFlow(emptyList())
        override fun observeIdsFor(wallpaperId: String): Flow<List<Long>> = MutableStateFlow(emptyList())
        override suspend fun create(name: String): Long = 1L
        override suspend fun rename(id: Long, name: String): Boolean = true
        override suspend fun delete(id: Long) {}
        override suspend fun addTo(collectionId: Long, wallpaperId: String) {}
        override suspend fun removeFrom(collectionId: Long, wallpaperId: String) {}
    }

    private class FakeOfflineImageStore : OfflineImageStore {
        override fun fileFor(id: String): File? = null
        override suspend fun save(wallpaper: Wallpaper): Boolean = true
        override fun delete(id: String) {}
    }

    private fun testContent() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        compose.setContent {
            KraftTheme {
                FavoritesScreen(
                    onOpenWallpaper = {},
                    gridState = rememberLazyStaggeredGridState(),
                    viewModel = FavoritesViewModel(
                        favoritesRepository = FakeFavoritesRepository(),
                        settingsRepository = FakeSettingsRepository(),
                        rotationStore = FakeRotationStore(),
                        collectionsRepository = FakeCollectionsRepository(),
                        favoriteImageStore = FakeOfflineImageStore(),
                    ),
                    connectivityViewModel = ConnectivityViewModel(ConnectivityObserver(context)),
                    collectionsVm = CollectionsViewModel(FakeCollectionsRepository()),
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun empty_state_title_is_displayed() {
        testContent()
        compose.onNodeWithText("No favorites yet").assertIsDisplayed()
    }

    @Test
    fun empty_state_message_is_displayed() {
        testContent()
        compose.onNodeWithText("Tap the heart on any wallpaper to save it here.").assertIsDisplayed()
    }
}
