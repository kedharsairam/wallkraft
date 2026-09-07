package com.wallkraft.app.presentation.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performLongClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftTheme
import com.wallkraft.app.domain.model.Category
import com.wallkraft.app.domain.model.WallhavenFilters
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Isolated UI tests for the search bar + filter entry point.
 *
 * Pure props-in/callbacks-out — no network, no container, no database.
 */
@RunWith(AndroidJUnit4::class)
class SearchFilterBarTest {

    @get:Rule
    val compose = createComposeRule()

    private fun resString(id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun setBar(
        query: String = "",
        onQueryChange: (String) -> Unit = {},
        onSearch: (String) -> Unit = {},
        onFiltersChange: (WallhavenFilters) -> Unit = {},
        totalResults: Int = 0,
        history: List<String> = emptyList(),
        onClearHistory: () -> Unit = {},
    ) {
        compose.setContent {
            KraftTheme {
                SearchFilterBar(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    filters = WallhavenFilters(),
                    onFiltersChange = onFiltersChange,
                    totalResults = totalResults,
                    history = history,
                    onClearHistory = onClearHistory,
                )
            }
        }
    }

    @Test
    fun typing_calls_onQueryChange_with_text() {
        val seen = mutableListOf<String>()
        setBar(onQueryChange = { seen += it })

        compose.onNodeWithText(resString(R.string.search_hint))
            .performTextInput("mountains")

        assertEquals(listOf("mountains"), seen)
    }

    @Test
    fun ime_search_calls_onSearch_with_query() {
        var searched: String? = null
        // "miku" matches no suggestion, so no row collides with the field text.
        setBar(query = "miku", onSearch = { searched = it })

        compose.onNodeWithText("miku").performImeAction()

        assertEquals("miku", searched)
    }

    @Test
    fun changing_chip_and_apply_commits_filters() {
        var committed: WallhavenFilters? = null
        setBar(onFiltersChange = { committed = it })

        // Open the panel, deselect Anime, Apply.
        compose.onNodeWithContentDescription(resString(R.string.filters))
            .performClick()
        compose.onNodeWithText(resString(R.string.category_anime))
            .performClick()
        compose.onNodeWithText(resString(R.string.filter_apply))
            .performScrollTo()
            .performClick()

        assertEquals(
            WallhavenFilters(categories = setOf(Category.General, Category.People)),
            committed,
        )
    }

    @Test
    fun selecting_color_and_apply_commits_colors() {        var committed: WallhavenFilters? = null
        setBar(onFiltersChange = { committed = it })

        // Open the panel, tap the Blue dot, Apply.
        compose.onNodeWithContentDescription(resString(R.string.filters))
            .performClick()
        compose.onNodeWithContentDescription(resString(R.string.color_blue))
            .performScrollTo()
            .performClick()
        compose.onNodeWithText(resString(R.string.filter_apply))
            .performScrollTo()
            .performClick()

        assertEquals("0066cc", committed?.colors)
    }

    @Test
    fun long_press_expands_shades_and_picks_exact_hex() {
        var committed: WallhavenFilters? = null
        setBar(onFiltersChange = { committed = it })

        // Open the panel, long-press Red to expand its shades, pick 990000.
        compose.onNodeWithContentDescription(resString(R.string.filters))
            .performClick()
        compose.onNodeWithContentDescription(resString(R.string.color_red))
            .performScrollTo()
            .performLongClick()
        compose.onNodeWithContentDescription("#990000", useUnmergedTree = true)
            .performScrollTo()
            .performClick()
        compose.onNodeWithText(resString(R.string.filter_apply))
            .performScrollTo()
            .performClick()

        assertEquals("990000", committed?.colors)
    }

    @Test
    fun tapping_history_item_searches_for_it() {
        var queried: String? = null
        var searched: String? = null
        // "oceanview" is unique: no other node collides with it.
        setBar(
            history = listOf("oceanview"),
            onQueryChange = { queried = it },
            onSearch = { searched = it },
        )

        compose.onNodeWithText(resString(R.string.search_hint)).performClick()
        compose.onNodeWithText("oceanview").performClick()

        assertEquals("oceanview", queried)
        assertEquals("oceanview", searched)
    }

    @Test
    fun dropdown_lists_history_items() {
        setBar(history = listOf("oceanview", "forestfire"))

        compose.onNodeWithText(resString(R.string.search_hint)).performClick()

        compose.onNodeWithText("oceanview").assertExists()
        compose.onNodeWithText("forestfire").assertExists()
    }

    @Test
    fun empty_query_with_no_history_shows_no_dropdown() {
        setBar()

        compose.onNodeWithText(resString(R.string.search_hint)).performClick()

        compose.onNodeWithText(resString(R.string.search_recent)).assertDoesNotExist()
    }

    @Test
    fun typing_filters_suggestions() {
        var searched: String? = null
        // Distinct words: nodes stay unique.
        setBar(
            history = listOf("oceanview", "forestfire"),
            onSearch = { searched = it },
        )

        compose.onNodeWithText(resString(R.string.search_hint)).performClick()
        compose.onNodeWithText(resString(R.string.search_hint)).performTextInput("oce")

        // "oceanview" matches and searches; "forestfire" is filtered out entirely.
        compose.onNodeWithText("oceanview").performClick()
        assertEquals("oceanview", searched)
        compose.onNodeWithText("forestfire").assertDoesNotExist()
    }

    @Test
    fun clear_history_calls_callback() {
        var clears = 0
        setBar(history = listOf("ocean"), onClearHistory = { clears++ })

        compose.onNodeWithText(resString(R.string.search_hint)).performClick()
        compose.onNodeWithText(resString(R.string.search_clear_history)).performClick()

        assertEquals(1, clears)
    }

    @Test
    fun count_shows_compact_total_when_known() {
        setBar(totalResults = 5085)

        // Decorative node (clearAndSetSemantics) — needs unmerged tree.
        compose.onNodeWithText("5.1k", useUnmergedTree = true).assertExists()
    }

    @Test
    fun count_hidden_when_total_unknown() {
        setBar(totalResults = 0)

        compose.onNodeWithText("5.1k", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun count_shows_alongside_query_text() {
        setBar(query = "miku", totalResults = 1_234_567)

        compose.onNodeWithText("miku").assertExists()
        compose.onNodeWithText("1.2m", useUnmergedTree = true).assertExists()
    }
}
