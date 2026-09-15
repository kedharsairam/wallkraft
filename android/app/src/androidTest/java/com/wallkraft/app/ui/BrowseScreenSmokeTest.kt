package com.wallkraft.app.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftTheme
import com.wallkraft.app.presentation.browse.BrowseScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalSharedTransitionApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BrowseScreenSmokeTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<TestActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

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

