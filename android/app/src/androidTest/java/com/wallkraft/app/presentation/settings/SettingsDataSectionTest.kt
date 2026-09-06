package com.wallkraft.app.presentation.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Isolated UI tests for the Settings data section.
 *
 * Pure props-in/callbacks-out — no ViewModel, no network, no database.
 */
@RunWith(AndroidJUnit4::class)
class SettingsDataSectionTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setSection(
        dataSaverMode: Boolean = false,
        onDataSaverChange: (Boolean) -> Unit = {},
        onClearCacheClick: () -> Unit = {},
    ) {
        compose.setContent {
            KraftTheme {
                SettingsDataSection(
                    dataSaverMode = dataSaverMode,
                    onDataSaverChange = onDataSaverChange,
                    cacheSizeText = "1.2 MB",
                    onClearCacheClick = onClearCacheClick,
                )
            }
        }
    }

    @Test
    fun shows_cache_size() {
        setSection()

        compose.onNodeWithText("1.2 MB", substring = true).assertIsDisplayed()
    }

    @Test
    fun toggle_click_reports_new_value() {
        val seen = mutableListOf<Boolean>()
        setSection(dataSaverMode = false, onDataSaverChange = { seen += it })

        compose.onNode(isToggleable()).performClick()

        assertEquals(listOf(true), seen)
    }

    @Test
    fun clear_cache_click_calls_callback() {
        var clicks = 0
        setSection(onClearCacheClick = { clicks++ })

        compose.onNodeWithText(
            ApplicationProvider.getApplicationContext<Context>().getString(R.string.clear_cache),
        ).performClick()

        assertEquals(1, clicks)
    }
}
