package com.wallkraft.app.presentation.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wallkraft.app.core.design.KraftTheme
import com.wallkraft.app.data.db.CollectionEntity
import com.wallkraft.app.data.db.CollectionWithItems
import com.wallkraft.app.data.prefs.RotationSettings
import com.wallkraft.app.domain.model.RotationMode
import com.wallkraft.app.domain.model.RotationSchedule
import com.wallkraft.app.domain.model.RotationTarget
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Isolated rotation section tests — callbacks only, no stores or workers.
 */
@RunWith(AndroidJUnit4::class)
class SettingsRotationSectionTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setSection(
        onSchedule: (RotationSchedule) -> Unit = {},
        onMode: (RotationMode) -> Unit = {},
        onTarget: (RotationTarget) -> Unit = {},
        onSource: (Long?) -> Unit = {},
        onRotateNow: () -> Unit = {},
    ) {
        compose.setContent {
            KraftTheme {
                SettingsRotationSection(
                    settings = RotationSettings(),
                    collections = listOf(
                        CollectionWithItems(
                            CollectionEntity(id = 9L, name = "Beach", createdAt = 1L),
                            emptyList(),
                        ),
                    ),
                    onSchedule = onSchedule,
                    onMode = onMode,
                    onTarget = onTarget,
                    onSource = onSource,
                    onRotateNow = onRotateNow,
                )
            }
        }
    }

    @Test
    fun schedule_chip_reports_choice() {
        var chosen: RotationSchedule? = null
        setSection(onSchedule = { chosen = it })

        compose.onNodeWithText("Daily").performClick()

        assertEquals(RotationSchedule.DAILY, chosen)
    }

    @Test
    fun mode_chip_reports_choice() {
        var chosen: RotationMode? = null
        setSection(onMode = { chosen = it })

        compose.onNodeWithText("Atmosphere").performClick()

        assertEquals(RotationMode.ATMOSPHERE, chosen)
    }

    @Test
    fun rotate_now_calls_callback() {
        var calls = 0
        setSection(onRotateNow = { calls++ })

        compose.onNodeWithText("Rotate now").performClick()

        assertEquals(1, calls)
    }

    @Test
    fun source_dialog_selects_collection() {
        var chosen: Long?? = null
        setSection(onSource = { chosen = it })

        // Open the source picker.
        compose.onNodeWithText("All favorites").performClick()
        // The dialog lists collections; tapping one reports its id.
        compose.onNodeWithText("Beach").performClick()

        assertEquals(9L, chosen)
    }
}
