package com.wallkraft.app.presentation.favorites

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wallkraft.app.core.design.KraftTheme
import com.wallkraft.app.data.cache.FavoriteOfflineRepair
import com.wallkraft.app.data.cache.OfflineImageStore
import com.wallkraft.app.domain.model.Wallpaper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.inject.Inject
import com.wallkraft.app.domain.repository.FavoritesRepository

/**
 * Download-all flow with a fake offline store — no network, no real files.
 *
 * Auto-repair is disabled so the test fully controls the timeline.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FavoritesDownloadAllTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Inject lateinit var favoritesRepository: FavoritesRepository

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
    }

    @Before
    fun setUp() {
        hiltRule.inject()
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
