package com.wallkraft.app.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing

/**
 * A curated color palette for the Wallhaven `colors` search parameter.
 *
 * Hex values MUST come from the API's fixed allowed set (see
 * https://wallhaven.cc/help/api) — any other value returns zero results.
 * Single-select: tapping the active color clears the filter.
 */
data class FilterColor(
    @StringRes val nameRes: Int,
    val hex: String,
)

val FilterColorPalette = listOf(
    FilterColor(R.string.color_red, "cc0000"),
    FilterColor(R.string.color_orange, "ff6600"),
    FilterColor(R.string.color_yellow, "ffff00"),
    FilterColor(R.string.color_green, "77cc33"),
    FilterColor(R.string.color_teal, "66cccc"),
    FilterColor(R.string.color_blue, "0066cc"),
    FilterColor(R.string.color_purple, "663399"),
    FilterColor(R.string.color_pink, "ea4c88"),
    FilterColor(R.string.color_brown, "663300"),
    FilterColor(R.string.color_black, "000000"),
    FilterColor(R.string.color_white, "ffffff"),
    FilterColor(R.string.color_grey, "999999"),
)

/**
 * Color dot row for the filter panel. Follows the same section pattern as
 * the chip rows (label + FlowRow + draft staging in the caller).
 *
 * @param selectedHex currently selected hex, or "" for none.
 * @param onSelect called with the new hex ("" when the active dot is tapped).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorFilterRow(
    selectedHex: String,
    onSelect: (String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Column {
        FilterSectionLabel(stringResource(R.string.filter_colors))
        Spacer(Modifier.height(KraftSpacing.Spacing4))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing6),
            verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing4),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FilterColorPalette.forEach { color ->
                val selected = color.hex == selectedHex
                val name = stringResource(color.nameRes)
                Box(
                    modifier = Modifier
                        .size(width = KraftSpacing.Spacing56, height = KraftSpacing.Spacing32)
                        .clip(RoundedCornerShape(KraftRadius.Small))
                        .background(Color(("FF" + color.hex).toLong(16)))
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(KraftRadius.Small),
                        )
                        .semantics { contentDescription = name }
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelect(if (selected) "" else color.hex)
                        },
                )
            }
        }
    }
}
