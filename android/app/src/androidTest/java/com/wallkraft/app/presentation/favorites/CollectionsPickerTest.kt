package com.wallkraft.app.presentation.favorites

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
import com.wallkraft.app.domain.repository.CollectionsRepository
import com.wallkraft.app.domain.repository.FavoritesRepository

/**
 * Collections end-to-end with a fake offline store:
 * create from the picker, toggle membership, count flows back to the strip.
 * No network anywhere (offline repair disabled, empty thumbnails).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CollectionsPickerTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Inject lateinit var favoritesRepository: FavoritesRepository
    @Inject lateinit var collectionsRepository: CollectionsRepository

    private class FakeStore : OfflineImageStore {
        override fun fileFor(id: String): File? = File(id)
        override suspend fun save(wallpaper: Wallpaper): Boolean = true
        override fun delete(id: String) {}
    }

    @Before
    fun setUp() {
        hiltRule.inject()
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
        compose.onNodeWithText("Collection name").performTextInput("Beach")
        compose.onNodeWithText("Create").performClick()
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
