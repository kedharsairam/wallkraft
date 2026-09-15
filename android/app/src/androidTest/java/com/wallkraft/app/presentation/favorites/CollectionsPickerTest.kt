package com.wallkraft.app.presentation.favorites

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wallkraft.app.core.design.KraftTheme
import com.wallkraft.app.data.cache.FavoriteOfflineRepair
import com.wallkraft.app.data.cache.OfflineImageStore
import com.wallkraft.app.data.db.WallKraftDatabase
import com.wallkraft.app.data.repository.CollectionsRepositoryImpl
import com.wallkraft.app.data.repository.FavoritesRepositoryImpl
import com.wallkraft.app.data.prefs.RotationSettings
import com.wallkraft.app.data.prefs.RotationSettingsStore
import com.wallkraft.app.domain.model.AppSettings
import com.wallkraft.app.domain.model.RotationMode
import com.wallkraft.app.domain.model.RotationSchedule
import com.wallkraft.app.domain.model.RotationTarget
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.repository.SettingsRepository
import com.wallkraft.app.presentation.common.ConnectivityViewModel
import com.wallkraft.app.presentation.favorites.CollectionsViewModel
import com.wallkraft.app.presentation.favorites.FavoritesViewModel
import com.wallkraft.app.util.ConnectivityObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import com.wallkraft.app.domain.repository.CollectionsRepository
import com.wallkraft.app.domain.repository.FavoritesRepository

/**
 * Collections end-to-end with a fake offline store:
 * create from the picker, toggle membership, count flows back to the strip.
 * No network anywhere (offline repair disabled, empty thumbnails).
 * Uses in-memory Room (no Hilt — manual construction).
 */
@RunWith(AndroidJUnit4::class)
class CollectionsPickerTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var favoritesRepository: FavoritesRepository
    private lateinit var collectionsRepository: CollectionsRepository

    private class FakeStore : OfflineImageStore {
        override fun fileFor(id: String): File? = File(id)
        override suspend fun save(wallpaper: Wallpaper): Boolean = true
        override fun delete(id: String) {}
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

    private fun testConnectivityViewModel(): ConnectivityViewModel {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        return ConnectivityViewModel(ConnectivityObserver(context))
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WallKraftDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        favoritesRepository = FavoritesRepositoryImpl(db.favoriteDao(), Json {}, FakeStore())
        collectionsRepository = CollectionsRepositoryImpl(db.collectionDao())
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Test
    fun create_toggle_and_count_round_trip() {
        runBlocking {
            favoritesRepository.observeAll().first().forEach { favorite ->
                favoritesRepository.remove(favorite.wallpaper.id)
            }
            favoritesRepository.add(Wallpaper(id = "t1"))
            favoritesRepository.add(Wallpaper(id = "t2"))
        }
        val topBarState = FavoritesTopBarState()

        compose.setContent {
            KraftTheme {
                FavoritesScreen(
                    onOpenWallpaper = {},
                    gridState = rememberLazyStaggeredGridState(),
                    offlineRepair = FavoriteOfflineRepair(FakeStore()),
                    autoRepairOffline = false,
                    topBarState = topBarState,
                    viewModel = FavoritesViewModel(
                        favoritesRepository = favoritesRepository,
                        settingsRepository = FakeSettingsRepository(),
                        rotationStore = FakeRotationStore(),
                        collectionsRepository = collectionsRepository,
                        favoriteImageStore = FakeStore(),
                    ),
                    connectivityViewModel = testConnectivityViewModel(),
                    collectionsVm = CollectionsViewModel(collectionsRepository),
                )
            }
        }

        // Strip with creation entry, no collections yet.
        compose.onNodeWithText("New collection").assertIsDisplayed()

        // Select all through the shared top-bar state, open the picker.
        // Selection syncs on recomposition — wait for it explicitly.
        topBarState.onEnterSelectionMode()
        compose.waitUntil(timeoutMillis = 5_000) { topBarState.selectionMode }
        topBarState.onAddToCollection()
        compose.onNodeWithText("Add to collection").assertIsDisplayed()

        // Create "Beach" inline, then toggle both selected wallpapers in.
        // The checkbox is tapped (not the row text) because the name also
        // exists on the strip card behind the dialog window.
        // Click first to guarantee focus, then type.
        compose.onNodeWithText("Collection name").performClick()
        compose.onNodeWithText("Collection name").performTextInput("Beach")
        compose.onNodeWithText("Create").performClick()
        // Wait for Room insert → picker list recompose with checkbox.
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(isToggleable()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodes(isToggleable())[0].performClick()
        compose.onNodeWithText("Done").performClick()

        // Membership flowed back through Room to the strip card count.
        compose.onNodeWithText("2 items").assertIsDisplayed()

        // And to the database itself.
        val memberIds = runBlocking {
            collectionsRepository.observeIdsFor("t1").first()
        }
        assertEquals(1, memberIds.size)
    }
}
