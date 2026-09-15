package com.wallkraft.app.presentation.favorites

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wallkraft.app.core.design.KraftTheme
import com.wallkraft.app.data.cache.FavoriteOfflineRepair
import com.wallkraft.app.data.cache.OfflineImageStore
import com.wallkraft.app.data.db.WallKraftDatabase
import com.wallkraft.app.data.prefs.RotationSettings
import com.wallkraft.app.data.prefs.RotationSettingsStore
import com.wallkraft.app.domain.model.RotationMode
import com.wallkraft.app.domain.model.RotationSchedule
import com.wallkraft.app.domain.model.RotationTarget
import com.wallkraft.app.data.repository.FavoritesRepositoryImpl
import com.wallkraft.app.domain.model.AppSettings
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.repository.CollectionsRepository
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
import com.wallkraft.app.domain.repository.FavoritesRepository

/**
 * Download-all flow with a fake offline store — no network, no real files.
 *
 * Auto-repair is disabled so the test fully controls the timeline.
 * Uses in-memory Room (no Hilt — manual construction).
 */
@RunWith(AndroidJUnit4::class)
class FavoritesDownloadAllTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var favoritesRepository: FavoritesRepository
    private lateinit var collectionsRepository: CollectionsRepository

    private class FakeStore : OfflineImageStore {
        val localIds = mutableSetOf<String>()
        val repaired = mutableListOf<String>()

        override fun fileFor(id: String): File? =
            if (id in localIds) File(id) else null

        override suspend fun save(wallpaper: Wallpaper): Boolean {
            repaired += wallpaper.id
            localIds += wallpaper.id
            return true
        }

        override fun delete(id: String) {
            localIds -= id
        }
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WallKraftDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        favoritesRepository = FavoritesRepositoryImpl(db.favoriteDao(), Json {}, FakeStore())
        collectionsRepository = com.wallkraft.app.data.repository.CollectionsRepositoryImpl(db.collectionDao())
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
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return ConnectivityViewModel(ConnectivityObserver(context))
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Test
    fun download_all_repairs_missing_and_snacks() {
        runBlocking {
            favoritesRepository.observeAll().first().forEach { favorite ->
                favoritesRepository.remove(favorite.wallpaper.id)
            }
            favoritesRepository.add(
                Wallpaper(id = "t1", path = "https://example.com/t1.jpg"),
            )
        }
        val fake = FakeStore()

        compose.setContent {
            KraftTheme {
                FavoritesScreen(
                    onOpenWallpaper = {},
                    gridState = rememberLazyStaggeredGridState(),
                    offlineRepair = FavoriteOfflineRepair(fake),
                    autoRepairOffline = false,
                    viewModel = FavoritesViewModel(
                        favoritesRepository = favoritesRepository,
                        settingsRepository = FakeSettingsRepository(),
                        rotationStore = FakeRotationStore(),
                        collectionsRepository = collectionsRepository,
                        favoriteImageStore = fake,
                    ),
                    connectivityViewModel = testConnectivityViewModel(),
                    collectionsVm = CollectionsViewModel(collectionsRepository),
                )
            }
        }

        // Missing local copy → Download-all visible. Tap → fake restores.
        // Repair runs on Dispatchers.IO (untracked by compose idling), so
        // wait for it explicitly before asserting.
        compose.onNodeWithText("Download all").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { fake.repaired.isNotEmpty() }

        assertEquals(listOf("t1"), fake.repaired)
        compose.onNodeWithText("Saved 1 wallpaper").assertIsDisplayed()
    }
}
