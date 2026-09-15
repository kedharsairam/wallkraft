package com.wallkraft.app.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wallkraft.app.core.design.KraftTheme
import com.wallkraft.app.presentation.favorites.FavoritesScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalSharedTransitionApi::class)
@RunWith(AndroidJUnit4::class)
class FavoritesScreenSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun empty_state_message_is_displayed() {
        compose.setContent {
            KraftTheme {
                FavoritesScreen(
                    onOpenWallpaper = {},
                    gridState = rememberLazyStaggeredGridState(),
                )
            }
        }

        compose.onNodeWithText("No favorites yet").assertIsDisplayed()
    }

    @Test
    fun empty_state_body_message_is_displayed() {
        compose.setContent {
            KraftTheme {
                FavoritesScreen(
                    onOpenWallpaper = {},
                    gridState = rememberLazyStaggeredGridState(),
                )
            }
        }

        compose.onNodeWithText("Tap the heart on any wallpaper to save it here.")
            .assertIsDisplayed()
    }

    @Test
    fun browse_action_button_is_displayed() {
        compose.setContent {
            KraftTheme {
                FavoritesScreen(
                    onOpenWallpaper = {},
                    gridState = rememberLazyStaggeredGridState(),
                )
            }
        }

        compose.onNodeWithText("Browse wallpapers").assertIsDisplayed()
    }
}
