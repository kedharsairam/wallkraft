package com.wallkraft.app.presentation.settings

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
 * Isolated UI tests for the Settings about section.
 *
 * Pure props-in/callbacks-out — no ViewModel, no network, no database.
 */
@RunWith(AndroidJUnit4::class)
class SettingsAboutSectionTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun share_crash_log_click_calls_callback() {
        var clicks = 0
        compose.setContent {
            KraftTheme {
                SettingsAboutSection(
                    githubUrl = "https://example.com",
                    onPrivacyClick = {},
                    onShareCrashLogClick = { clicks++ },
                )
            }
        }

        compose.onNodeWithText("Share crash log").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun privacy_click_calls_callback() {
        var clicks = 0
        compose.setContent {
            KraftTheme {
                SettingsAboutSection(
                    githubUrl = "https://example.com",
                    onPrivacyClick = { clicks++ },
                    onShareCrashLogClick = {},
                )
            }
        }

        compose.onNodeWithText("Privacy policy").performClick()

        assertEquals(1, clicks)
    }
}
