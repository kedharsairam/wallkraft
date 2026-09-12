package com.wallkraft.app.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing

/**
 * Grouped color palette for the Wallhaven `colors` search parameter.
 *
 * The API accepts 29 fixed hexes (see https://wallhaven.cc/help/api) — any
 * other value returns zero results. Showing 29 dots would flood the panel,
 * so they are grouped into 8 families (12 curated dots → 8). Single-tap
 * picks the family default; long-press expands the family's shades.
 *
 * Coverage (all 29 API hexes, nothing dropped):
 * - Red: 660000, 990000, cc0000, cc3333
 * - Purple/Pink: ea4c88, 993399, 663399, 333399
 * - Blue/Teal: 0066cc, 0099cc, 66cccc
 * - Green: 77cc33, 669900, 336600
 * - Yellow/Olive: ffff00, ffcc33, cccc33, 999900, 666600
 * - Orange/Brown: ff9900, ff6600, cc6633, 996633, 663300
 * - Black/Slate: 000000, 424153
 * - White/Grey: ffffff, cccccc, 999999
 *
 * Single-select: tapping the active dot (or shade) clears the filter.
 */
data class FilterColorGroup(
    @StringRes val nameRes: Int,
    val defaultHex: String,
    val shades: List<String>,
)

val FilterColorGroups = listOf(
    FilterColorGroup(R.string.color_red, "cc0000", listOf("660000", "990000", "cc0000", "cc3333")),
    FilterColorGroup(R.string.color_purple, "663399", listOf("ea4c88", "993399", "663399", "333399")),
    FilterColorGroup(R.string.color_blue, "0066cc", listOf("0066cc", "0099cc", "66cccc")),
    FilterColorGroup(R.string.color_green, "77cc33", listOf("77cc33", "669900", "336600")),
    FilterColorGroup(R.string.color_yellow, "ffff00", listOf("ffff00", "ffcc33", "cccc33", "999900", "666600")),
    FilterColorGroup(R.string.color_orange, "ff6600", listOf("ff9900", "ff6600", "cc6633", "996633", "663300")),
    FilterColorGroup(R.string.color_black, "000000", listOf("000000", "424153")),
    FilterColorGroup(R.string.color_white, "ffffff", listOf("ffffff", "cccccc", "999999")),
)

/**
 * Color dot row for the filter panel. Follows the same section pattern as
 * the chip rows (label + content + draft staging in the caller).
 *
 * Base row: 8 family dots in one line. Single-tap selects/clears WITHOUT
 * moving the strip (no layout jump); long-press expands/collapses that
 * family's shades. The strip is always one row (max 5 shades fit), so
 * switching families never shifts content below either — only an explicit
 * long-press open/close moves anything, like a disclosure.
 *
 * @param selectedHex currently selected hex, or "" for none.
 * @param onSelect called with the new hex ("" when the active dot is tapped).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ColorFilterRow(
    selectedHex: String,
    onSelect: (String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    // Which family's shades are expanded. Auto-follows the selection so a
    // restored filter shows its shades; manual long-press peeks otherwise.
    var expandedKey by remember(selectedHex) {
        mutableStateOf(FilterColorGroups.firstOrNull { selectedHex in it.shades }?.defaultHex)
    }
    Column {
        FilterSectionLabel(stringResource(R.string.filter_colors))
        // 8dp, not the usual 4dp: solid full-bleed color masses compress
        // perceived air vs airy outlined chips, so color-touching gaps get
        // one rhythm step more. Rule: gaps touching color are 8dp, gaps
        // touching chips/text stay 4dp like the rest of the panel.
        Spacer(Modifier.height(KraftSpacing.Spacing8))
        // Single line: 8 dots share the width equally (weight), so every
        // phone shows one full row. Small phones get ~36dp targets — below
        // the 44dp ideal, but that's the single-line tradeoff Kedhar chose.
        Row(
            horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FilterColorGroups.forEach { group ->
                val active = selectedHex in group.shades
                val name = stringResource(group.nameRes)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(KraftSpacing.TouchTarget),
                ) {
                    ColorDot(
                        hex = group.defaultHex,
                        contentDesc = name,
                        selected = active,
                        modifier = Modifier.fillMaxSize(),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (active) {
                                // Clearing also collapses — nothing to show shades for.
                                onSelect("")
                                expandedKey = null
                            } else {
                                onSelect(group.defaultHex)
                                // Slide an already-open strip to this family
                                // (same one-row height — zero shift). A closed
                                // strip stays closed: tap selects, long-press
                                // discloses. No jump on either path.
                                if (expandedKey != null) expandedKey = group.defaultHex
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            expandedKey = if (expandedKey == group.defaultHex) null else group.defaultHex
                        },
                    )
                }
            }
        }
        // Shade strip for the expanded family (smaller dots, exact hexes).
        // Shade contentDescriptions use the raw hex — no new translatable
        // strings needed across locales.
        val expanded = FilterColorGroups.firstOrNull { it.defaultHex == expandedKey }
        if (expanded != null) {
            // 8dp like the label gap above — color-touching gaps breathe more.
            Spacer(Modifier.height(KraftSpacing.Spacing8))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                modifier = Modifier.fillMaxWidth(),
            ) {
                expanded.shades.forEach { hex ->
                    val shadeSelected = hex == selectedHex
                    ColorDot(
                        hex = hex,
                        contentDesc = "#$hex",
                        selected = shadeSelected,
                        modifier = Modifier.size(
                            width = KraftSpacing.Spacing40,
                            height = 32.dp,
                        ),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelect(if (shadeSelected) "" else hex)
                        },
                        onLongClick = null,
                    )
                }
            }
        }
        // Trailing 8dp: closes the color block with the same air it opens
        // with, so the divider below sits as easy as under chip sections.
        Spacer(Modifier.height(KraftSpacing.Spacing8))
    }
}

/**
 * Single color dot with Apple-style selection: unselected gets a 1dp outline;
 * selected gets a 2dp primary halo separated by a 2dp panel-color gap, so the
 * halo stays visible on every fill — including white-on-white.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColorDot(
    hex: String,
    contentDesc: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    val shape = RoundedCornerShape(KraftRadius.Small)
    val fill = Color(("FF" + hex).toLong(16))
    val gesture = Modifier.combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
        role = Role.Button,
    )
    if (selected) {
        // Halo sandwich: primary → panel gap → fill. The gap is what saves
        // white (ffffff fill, a white halo would melt without the gap).
        Box(
            modifier = modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.primary)
                .padding(2.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = contentDesc
                    role = Role.RadioButton
                    this.selected = true
                }
                .then(gesture),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceBright)
                    .padding(2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape)
                        .background(fill),
                )
            }
        }
    } else {
        // Borderless fill — no outline. Shape reads via the fill itself.
        Box(
            modifier = modifier
                .clip(shape)
                .background(fill)
                .semantics(mergeDescendants = true) {
                    contentDescription = contentDesc
                    role = Role.RadioButton
                    this.selected = false
                }
                .then(gesture),
        )
    }
}
