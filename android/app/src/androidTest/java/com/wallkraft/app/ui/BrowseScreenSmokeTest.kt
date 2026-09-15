package com.wallkraft.app.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftTheme
import com.wallkraft.app.presentation.browse.BrowseScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalSharedTransitionApi::class)
@RunWith(AndroidJUnit4::class)
class BrowseScreenSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun search_bar_is_displayed() {
        compose.setContent {
            KraftTheme {
                BrowseScreen(onOpenWallpaper = {})
            }
        }

        compose.onNodeWithText("Search").assertIsDisplayed()
    }

    @Test
    fun filter_button_is_displayed() {
        compose.setContent {
            KraftTheme {
                BrowseScreen(onOpenWallpaper = {})
            }
        }

        compose.onNodeWithContentDescription("Filters").assertIsDisplayed()
    }

    @Test
    fun loading_state_shows_shimmer_grid() {
        compose.setContent {
            KraftTheme {
                BrowseScreen(onOpenWallpaper = {})
            }
        }

        compose.onNodeWithContentDescription("Loading wallpapers").assertIsDisplayed()
    }
}
