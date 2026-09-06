package com.wallkraft.app.presentation.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wallkraft.app.core.design.KraftTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Isolated UI tests for the generic empty state.
 *
 * Pure props-in/callbacks-out — no network, no container, no database.
 */
@RunWith(AndroidJUnit4::class)
class EmptyStateTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun shows_title_and_message() {
        compose.setContent {
            KraftTheme {
                EmptyState(title = "No results", message = "Try another search")
            }
        }

        compose.onNodeWithText("No results").assertIsDisplayed()
        compose.onNodeWithText("Try another search").assertIsDisplayed()
    }

    @Test
    fun action_button_click_calls_onAction() {
        var clicks = 0
        compose.setContent {
            KraftTheme {
                EmptyState(
                    title = "No results",
                    message = "Try another search",
                    actionLabel = "Retry",
                    onAction = { clicks++ },
                )
            }
        }

        compose.onNodeWithText("Retry").performClick()

        assertEquals(1, clicks)
    }
}
