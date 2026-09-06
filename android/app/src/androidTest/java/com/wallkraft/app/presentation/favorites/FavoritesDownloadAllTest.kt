package com.wallkraft.app.presentation.favorites

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.wallkraft.app.AppContainer
import com.wallkraft.app.core.design.KraftTheme
import com.wallkraft.app.data.cache.FavoriteOfflineRepair
import com.wallkraft.app.data.cache.OfflineImageStore
import com.wallkraft.app.domain.model.Wallpaper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Download-all flow with a fake offline store — no network, no real files.
 *
 * Auto-repair is disabled so the test fully controls the timeline.
 */
@RunWith(AndroidJUnit4::class)
class FavoritesDownloadAllTest {

    @get:Rule
    val compose = createComposeRule()

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

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Test
    fun download_all_repairs_missing_and_snacks() {
        val container = AppContainer(ApplicationProvider.getApplicationContext<Context>())
        runBlocking {
            // The test shares the app's real database — start clean so manual
            // testing leftovers can't leak into assertions.
            container.favoritesRepository.observeAll().first().forEach { favorite ->
                container.favoritesRepository.remove(favorite.wallpaper.id)
            }
            container.favoritesRepository.add(
                Wallpaper(id = "t1", path = "https://example.com/t1.jpg"),
            )
        }
        val fake = FakeStore()

        compose.setContent {
            KraftTheme {
                FavoritesScreen(
                    container = container,
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
