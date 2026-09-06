package com.wallkraft.app.presentation.favorites

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wallkraft.app.core.design.KraftTheme
import com.wallkraft.app.data.db.CollectionEntity
import com.wallkraft.app.data.db.CollectionItemEntity
import com.wallkraft.app.data.db.CollectionWithItems
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Isolated strip tests — covers are empty (placeholder only, no network).
 */
@RunWith(AndroidJUnit4::class)
class CollectionStripTest {

    @get:Rule
    val compose = createComposeRule()

    private val beach = CollectionWithItems(
        CollectionEntity(id = 7L, name = "Beach", createdAt = 1L),
        listOf(CollectionItemEntity(7L, "w1")),
    )

    private fun setStrip(
        activeId: Long? = null,
        onSelect: (Long?) -> Unit = {},
        onNew: () -> Unit = {},
        onLongPress: (Long) -> Unit = {},
    ) {
        compose.setContent {
            KraftTheme {
                CollectionStrip(
                    collections = listOf(beach),
                    covers = emptyMap(),
                    activeId = activeId,
                    onSelect = onSelect,
                    onNew = onNew,
                    onLongPress = onLongPress,
                )
            }
        }
    }

    @Test
    fun renders_name_count_and_new() {
        setStrip()

        compose.onNodeWithText("Beach").assertIsDisplayed()
        compose.onNodeWithText("1 item").assertIsDisplayed()
        compose.onNodeWithText("New collection").assertIsDisplayed()
    }

    @Test
    fun tap_card_selects_tap_again_clears() {
        val selected = mutableListOf<Long?>()
        val active = mutableStateOf<Long?>(null)
        compose.setContent {
            KraftTheme {
                CollectionStrip(
                    collections = listOf(beach),
                    covers = emptyMap(),
                    activeId = active.value,
                    onSelect = { selected += it; active.value = it },
                    onNew = {},
                    onLongPress = {},
                )
            }
        }

        compose.onNodeWithText("Beach").performClick()
        compose.onNodeWithText("Beach").performClick()

        assertEquals(listOf(7L, null), selected)
    }

    @Test
    fun new_card_calls_onNew() {
        var news = 0
        setStrip(onNew = { news++ })

        compose.onNodeWithText("New collection").performClick()

        assertEquals(1, news)
    }

    @Test
    fun rename_dialog_saves_trimmed_name() {
        var saved: String? = null
        compose.setContent {
            KraftTheme {
                RenameCollectionDialog(
                    title = "Rename dialog",
                    current = "Beach",
                    onDismiss = {},
                    onSave = { saved = it },
                )
            }
        }

        // Field starts pre-filled with the cursor at the start in tests,
        // so typed text lands in front.
        compose.onNodeWithText("Beach").performTextInput("!!")
        compose.onNodeWithText("Rename").performClick()

        assertEquals("!!Beach", saved)
    }

    @Test
    fun delete_dialog_confirms() {
        var confirmed = 0
        compose.setContent {
            KraftTheme {
                DeleteCollectionDialog(
                    name = "Beach",
                    onDismiss = {},
                    onConfirm = { confirmed++ },
                )
            }
        }

        compose.onNodeWithText("Delete").performClick()

        assertEquals(1, confirmed)
    }
}
