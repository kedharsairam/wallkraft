package com.wallkraft.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.core.design.KraftColors
import com.wallkraft.app.core.design.KraftIconSize
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.core.design.KraftTypeScale
import com.wallkraft.app.domain.model.Category
import com.wallkraft.app.domain.model.Orientation
import com.wallkraft.app.domain.model.Purity
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.util.displayName

private val PillShape = RoundedCornerShape(KraftRadius.Pill)
private val PanelShape = RoundedCornerShape(bottomStart = KraftRadius.Large, bottomEnd = KraftRadius.Large)

/** Chip colors — dark theme only. */
@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = KraftColors.ChipSelectedContainer,
    selectedLabelColor = KraftColors.ChipSelectedLabel,
)

/** Purity SFW chip colors — dark theme only. */
@Composable
private fun puritySfwChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = KraftColors.PuritySfwContainer,
    selectedLabelColor = KraftColors.PuritySfwLabel,
)

/** Purity sketchy chip colors — dark theme only. */
@Composable
private fun puritySketchyChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = KraftColors.PuritySketchyContainer,
    selectedLabelColor = KraftColors.PuritySketchyLabel,
)

/** Purity NSFW chip colors — dark theme only. */
@Composable
private fun purityNsfwChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = KraftColors.PurityNsfwContainer,
    selectedLabelColor = KraftColors.PurityNsfwLabel,
)

/**
 * Clean search + filter bar.
 *
 * **Idle** — pill search bar (magnifying glass on right, "Search" placeholder)
 *           + grey rounded filter button.
 * **Focused** — search bar shrinks, a blue magnifying-glass circle slides in
 *               between the bar and the filter button.
 * **Filter open** — panel drops down from right below the bar.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SearchFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    filters: WallhavenFilters,
    onFiltersChange: (WallhavenFilters) -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    hasApiKey: Boolean = false,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    var isFocused by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var barHeight by remember { mutableIntStateOf(0) }

    // Dismiss filter panel when search bar loses focus (e.g. user taps
    // outside on the browse area). Only fires on focus LOSS — not on focus
    // gain — so tapping the filter button while the search bar is focused
    // doesn't race with the toggle.
    LaunchedEffect(isFocused) {
        if (!isFocused && showFilters) {
            showFilters = false
        }
    }

    // Notify parent when panel is dismissed (e.g. on outside tap)
    LaunchedEffect(showFilters) {
        if (!showFilters) onDismiss?.invoke()
    }

    // Draft filters — staged inside the panel, only committed on Apply.
    var draftFilters by remember { mutableStateOf(filters) }
    androidx.compose.runtime.LaunchedEffect(showFilters) {
        if (showFilters) draftFilters = filters
    }

    Box(modifier = modifier) {
        // ── Main content ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .background(KraftColors.SurfaceSecondary)
                .onGloballyPositioned { coordinates ->
                    barHeight = coordinates.size.height
                },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = KraftSpacing.Spacing16, vertical = KraftSpacing.Spacing8),
            ) {
                // ── Search bar ──────────────────────────────────────────
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Search,
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboard?.hide()
                            onSearch(query)
                        },
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .height(KraftSpacing.SearchBarHeight)
                        .onFocusChanged { isFocused = it.isFocused },
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(KraftSpacing.TouchTarget)
                                .clip(PillShape)
                                // Search bar = #1C1C1E for depth against #000000 page.
                                .background(KraftColors.SearchBar)
                                .padding(horizontal = KraftSpacing.Spacing16),
                        ) {
                            Box(
                                contentAlignment = Alignment.CenterStart,
                                modifier = Modifier.weight(1f),
                            ) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.search_hint),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                innerTextField()
                            }
                            // Magnifying glass — only when NOT focused
                            if (!isFocused) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    },
                )

                Spacer(Modifier.width(KraftSpacing.Spacing8))

                // ── Search button (appears on focus) ────────────────────
                AnimatedVisibility(
                    visible = isFocused,
                    enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                    exit = shrinkVertically(tween(200)) + fadeOut(tween(200)),
                ) {
                    Row {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(KraftSpacing.TouchTarget)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    keyboard?.hide()
                                    focusManager.clearFocus()
                                    onSearch(query)
                                },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = stringResource(R.string.search_hint),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(KraftIconSize.Medium),
                            )
                        }
                        Spacer(Modifier.width(KraftSpacing.Spacing8))
                    }
                }

                // ── Filter button ───────────────────────────────────────
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(KraftSpacing.TouchTarget)
                        .clip(PillShape)
                        .background(
                            if (showFilters) MaterialTheme.colorScheme.primary
                            else KraftColors.Surface,
                        )
                        .clickable {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            keyboard?.hide()
                            focusManager.clearFocus()
                            showFilters = !showFilters
                        },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = stringResource(R.string.filters),
                        tint = if (showFilters) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(KraftIconSize.Medium),
                    )
                }
            }

            HorizontalDivider(
                // Separator = outline color (~35% of #545458)
                color = MaterialTheme.colorScheme.outline,
            )
        }

        // ── Filter panel (drops down from below the bar) ────────────────
        // The panel overlays on top of the content below without pushing it
        // down. layout{} reports zero height so the parent Box doesn't grow,
        // but the child is still drawn at the correct position via place().
        // This is the standard pattern for dropdown menus and popover panels.
        // Max height is dynamic: screen height minus search bar and bottom bar.
        val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
        val panelMaxHeight = screenHeightDp - KraftSpacing.SearchBarHeight - 48.dp
        AnimatedVisibility(
            visible = showFilters,
            enter = expandVertically(tween(250)) + fadeIn(tween(250)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(200)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = panelMaxHeight)
                .zIndex(1f)
                .offset { IntOffset(0, barHeight) } // position below the measured bar
                .layout { measurable, constraints ->
                    // Measure the child but report zero size to parent
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, 0) {
                        placeable.place(0, 0)
                    }
                },
        ) {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, PanelShape)
                    .clip(PanelShape)
                    // Elevated surface for visual separation from content.
                    .background(KraftColors.SurfaceSecondary)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downY = down.position.y
                            var draggedUp = false
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull() ?: break
                                val dy = change.position.y - downY
                                if (dy < -100f) {
                                    draggedUp = true
                                    change.consume()
                                    break
                                }
                                if (!change.pressed) break
                            }
                            if (draggedUp) {
                                showFilters = false
                            }
                        }
                    }
                    .verticalScroll(scrollState)
                    .padding(horizontal = KraftSpacing.Spacing16, vertical = KraftSpacing.Spacing16),
            ) {
                // ── Title ──────────────────────────────────────────────
                Text(
                    text = stringResource(R.string.filters),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(KraftSpacing.Spacing20))

                // ── Categories ─────────────────────────────────────────
                FilterSectionLabel(stringResource(R.string.filter_categories))
                Spacer(Modifier.height(KraftSpacing.Spacing8))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Category.entries.forEach { cat ->
                        val checked = cat in draftFilters.categories
                        FilterChip(
                            selected = checked,
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                val current = draftFilters.categories
                                val updated = if (cat in current) {
                                    if (current.size > 1) current - cat else current
                                } else current + cat
                                draftFilters = draftFilters.copy(categories = updated)
                            },
                            label = { Text(cat.displayName()) },
                            colors = chipColors(),
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(KraftSpacing.Spacing16))

                // ── Purity ─────────────────────────────────────────────
                FilterSectionLabel(stringResource(R.string.filter_purity))
                Spacer(Modifier.height(KraftSpacing.Spacing8))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Purity.entries.forEach { p ->
                        val isNsfwLocked = p == Purity.NSFW && !hasApiKey
                        val checked = p in draftFilters.purity && !isNsfwLocked
                        val currentChipColors = when {
                            p == Purity.SFW -> puritySfwChipColors()
                            p == Purity.Sketchy -> puritySketchyChipColors()
                            isNsfwLocked -> FilterChipDefaults.filterChipColors(
                                containerColor = KraftColors.PurityNsfwContainer.copy(alpha = 0.15f),
                                labelColor = KraftColors.PurityNsfwLabel.copy(alpha = 0.4f),
                                disabledContainerColor = KraftColors.PurityNsfwContainer.copy(alpha = 0.15f),
                                disabledLabelColor = KraftColors.PurityNsfwLabel.copy(alpha = 0.4f),
                            )
                            else -> purityNsfwChipColors()
                        }
                        FilterChip(
                            selected = checked,
                            onClick = {
                                if (isNsfwLocked) return@FilterChip
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                val current = draftFilters.purity
                                val updated = if (p in current) {
                                    if (current.size > 1) current - p else current
                                } else current + p
                                draftFilters = draftFilters.copy(purity = updated)
                            },
                            label = { Text(p.displayName()) },
                            leadingIcon = if (isNsfwLocked) {
                                { Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(KraftIconSize.Tiny)) }
                            } else null,
                            colors = currentChipColors,
                            enabled = !isNsfwLocked,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(KraftSpacing.Spacing16))

                // ── Sorting ────────────────────────────────────────────
                FilterSectionLabel(stringResource(R.string.filter_sorting))
                Spacer(Modifier.height(KraftSpacing.Spacing8))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing4),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Sorting.entries.forEach { s ->
                        FilterChip(
                            selected = draftFilters.sorting == s,
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                draftFilters = draftFilters.copy(sorting = s)
                            },
                            label = { Text(s.displayName()) },
                            colors = chipColors(),
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(KraftSpacing.Spacing16))

                // ── Orientation ────────────────────────────────────────
                FilterSectionLabel(stringResource(R.string.filter_orientation))
                Spacer(Modifier.height(KraftSpacing.Spacing8))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Orientation.entries.forEach { o ->
                        FilterChip(
                            selected = draftFilters.orientation == o,
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                draftFilters = draftFilters.copy(orientation = o)
                            },
                            label = { Text(o.displayName()) },
                            colors = chipColors(),
                        )
                    }
                }
                Spacer(Modifier.height(KraftSpacing.Spacing24))

                // ── Actions ────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing12),
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            draftFilters = WallhavenFilters(query = filters.query)
                        },
                        modifier = Modifier.weight(1f).height(KraftSpacing.TouchTarget),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                    ) { Text(stringResource(R.string.filter_reset)) }
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            if (draftFilters != filters) onFiltersChange(draftFilters)
                            showFilters = false
                        },
                        enabled = draftFilters != filters,
                        modifier = Modifier.weight(1f).height(KraftSpacing.TouchTarget),
                    ) { Text(stringResource(R.string.filter_apply)) }
                }
            }
        }
    }
}

@Composable
private fun FilterSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
