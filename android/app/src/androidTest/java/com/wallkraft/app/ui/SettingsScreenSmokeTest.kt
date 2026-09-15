package com.wallkraft.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftTheme
import com.wallkraft.app.presentation.settings.SettingsScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsScreenSmokeTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun resString(id: Int): String =
        androidx.test.core.app.ApplicationProvider
            .getApplicationContext<android.content.Context>()
            .getString(id)

    @Test
    fun browsing_section_is_displayed() {
        compose.setContent {
            KraftTheme {
                SettingsScreen()
            }
        }

        compose.onNodeWithText(resString(R.string.browsing_title)).assertIsDisplayed()
    }

    @Test
    fun data_section_is_displayed() {
        compose.setContent {
            KraftTheme {
                SettingsScreen()
            }
        }

        compose.onNodeWithText(resString(R.string.data_title)).assertIsDisplayed()
    }

    @Test
    fun advanced_section_is_displayed() {
        compose.setContent {
            KraftTheme {
                SettingsScreen()
            }
        }

        compose.onNodeWithText(resString(R.string.advanced_title))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun api_key_title_is_displayed() {
        compose.setContent {
            KraftTheme {
                SettingsScreen()
            }
        }

        compose.onNodeWithText(resString(R.string.api_key_title))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun about_section_is_displayed() {
        compose.setContent {
            KraftTheme {
                SettingsScreen()
            }
        }

        compose.onNodeWithText(resString(R.string.about_title))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun support_section_is_displayed() {
        compose.setContent {
            KraftTheme {
                SettingsScreen()
            }
        }

        compose.onNodeWithText(resString(R.string.buy_me_a_coffee_title))
            .performScrollTo()
            .assertIsDisplayed()
    }
}
