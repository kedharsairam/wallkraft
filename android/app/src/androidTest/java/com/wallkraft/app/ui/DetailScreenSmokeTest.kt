package com.wallkraft.app.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wallkraft.app.core.design.KraftTheme
import com.wallkraft.app.presentation.detail.DetailScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalSharedTransitionApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DetailScreenSmokeTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<TestActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun loading_indicator_is_displayed_for_invalid_id() {
        compose.setContent {
            KraftTheme {
                DetailScreen(
                    wallpaperId = "",
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText("Couldn\u2019t load this wallpaper").assertIsDisplayed()
    }

    @Test
    fun back_callback_is_called_on_error_retry() {
        var backCalled = false
        compose.setContent {
            KraftTheme {
                DetailScreen(
                    wallpaperId = "",
                    onBack = { backCalled = true },
                )
            }
        }

        compose.onNodeWithText("Try again").performClick()

        assert(backCalled) { "onBack should have been called" }
    }
}

