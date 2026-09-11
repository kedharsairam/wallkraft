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
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import com.wallkraft.app.domain.model.TopRange
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.util.displayName
import com.wallkraft.app.util.formatCount

private val PillShape = RoundedCornerShape(KraftRadius.Pill)
private val PanelShape = RoundedCornerShape(bottomStart = KraftRadius.Large, bottomEnd = KraftRadius.Large)

/**
 * Clean search + filter bar.
 *
 * **Idle** — pill search bar ("Search" placeholder or query text, total count
 *           on the right like "5.1k") + grey rounded filter button.
 *           No magnifier icon in the bar — the count owns the right edge.
 * **Focused** — count hides (typing needs the space), search bar shrinks, a
 *               blue magnifying-glass circle slides in between the bar and
 *               the filter button. Suggestions dropdown appears below.
 * **Loading / unknown** — totalResults = 0, so no count is shown (never a
 *           stale total — ViewModel resets to 0 on every new search).
 * **Empty / error** — same: no count, hint or query text only.
 * **Filter open** — focus is cleared on open, so the count stays visible.
 * **Long query + count** — query scrolls horizontally (singleLine), count is
 *           fixed-width short (max ~4 chars via formatCount) with 8dp gap,
 *           never wraps or pushes the bar taller.
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
    /** Total result count for the current listing — shown at the bar's right edge (0 = unknown). */
    totalResults: Int = 0,
    onDismiss: (() -> Unit)? = null,
    hasApiKey: Boolean = false,
    // Suggestion source: explicit search history only (typed + submitted).
    // Nothing auto-collected — no session tags, no tapped tags.
    history: List<String> = emptyList(),
    onClearHistory: () -> Unit = {},
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
                .background(Color.Transparent)
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
                            // Result count — idle only, known total only. Hidden while
                            // focused (typing context), while loading, and on
                            // empty/error states. Decorative: the grid itself
                            // carries the info for screen readers.
                            // Fixed short width (formatCount max ~4 chars like
                            // "5.1k" / "1.2m"), 8dp gap so a long query never
                            // collides — query scrolls, count stays pinned.
                            if (!isFocused && totalResults > 0) {
                                Spacer(Modifier.width(KraftSpacing.Spacing8))
                                Text(
                                    text = formatCount(totalResults),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier.clearAndSetSemantics {},
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

            // No divider — matches bottom pill (no hairline, just frost stack)
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
                    // Frosted like top/bottom but more opaque for readability:
                    // top/bottom are small bars (translucent is fine), this
                    // panel is large and needs ~95% opacity or text is lost
                    // behind the grid (see screenshot). Same hue stack as top/bottom
                    // but with higher alphas + a solid base.
                    .background(Color.Black.copy(alpha = 0.55f), PanelShape)
                    .background(Color.White.copy(alpha = 0.35f), PanelShape)
                    .background(Color(0xFF3A3A3C).copy(alpha = 0.85f), PanelShape)
                    .background(Color(0xFF2C2C2E).copy(alpha = 0.65f), PanelShape)
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
                Spacer(Modifier.height(KraftSpacing.Spacing8))

                // ── Categories ─────────────────────────────────────────
                FilterSectionLabel(stringResource(R.string.filter_categories))
                Spacer(Modifier.height(KraftSpacing.Spacing6))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing6),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing6),
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
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = KraftSpacing.Spacing6),
                    color = MaterialTheme.colorScheme.outline,
                )

                // ── Purity ─────────────────────────────────────────────
                FilterSectionLabel(stringResource(R.string.filter_purity))
                Spacer(Modifier.height(KraftSpacing.Spacing6))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing6),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing6),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Purity.entries.forEach { p ->
                        val isNsfwLocked = p == Purity.NSFW && !hasApiKey
                        val checked = p in draftFilters.purity && !isNsfwLocked
                        val currentChipColors = when {
                            p == Purity.SFW -> puritySfwChipColors()
                            p == Purity.Sketchy -> puritySketchyChipColors()
                            isNsfwLocked -> FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = KraftSpacing.Spacing6),
                    color = MaterialTheme.colorScheme.outline,
                )

                // ── Orientation ────────────────────────────────────────
                FilterSectionLabel(stringResource(R.string.filter_orientation))
                Spacer(Modifier.height(KraftSpacing.Spacing6))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing6),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing6),
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
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = KraftSpacing.Spacing6),
                    color = MaterialTheme.colorScheme.outline,
                )

                // ── Sorting (wallhaven.cc order) ─────────────────────────
                FilterSectionLabel(stringResource(R.string.filter_sorting))
                Spacer(Modifier.height(KraftSpacing.Spacing6))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing6),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing6),
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

                // ── Top range — only for Toplist (like wallhaven.cc) ─────
                AnimatedVisibility(
                    visible = draftFilters.sorting == Sorting.Toplist,
                    enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                    exit = shrinkVertically(tween(200)) + fadeOut(tween(200)),
                ) {
                    Column {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = KraftSpacing.Spacing8),
                            color = MaterialTheme.colorScheme.outline,
                        )
                        FilterSectionLabel(stringResource(R.string.filter_top_range))
                        Spacer(Modifier.height(KraftSpacing.Spacing6))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing6),
                            verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing6),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            TopRange.entries.forEach { t ->
                                FilterChip(
                                    selected = draftFilters.topRange == t,
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        draftFilters = draftFilters.copy(topRange = t)
                                    },
                                    label = { Text(t.displayName()) },
                                    colors = chipColors(),
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = KraftSpacing.Spacing6),
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.height(KraftSpacing.Spacing8))

                // ── Actions ────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
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

        // ── Suggestions dropdown (explicit history only) ──────────────
        // Same overlay pattern as the filter panel: zero reported height so
        // the bar never shifts, positioned below the measured bar. Shown only
        // while the field is focused and the filter panel is closed — focus
        // loss hides it automatically, so no dismiss handling is needed.
        // Empty query with no history shows nothing.
        val trimmedQuery = query.trim()
        val typedMatches = remember(trimmedQuery, history) {
            if (trimmedQuery.isEmpty()) emptyList()
            else history
                .filter { it.contains(trimmedQuery, ignoreCase = true) }
                .distinctBy { it.lowercase() }
                .take(8)
        }
        fun selectSuggestion(text: String) {
            onQueryChange(text)
            onSearch(text)
            keyboard?.hide()
            focusManager.clearFocus()
        }
        AnimatedVisibility(
            visible = isFocused && !showFilters &&
                (trimmedQuery.isNotEmpty() && typedMatches.isNotEmpty() ||
                    trimmedQuery.isEmpty() && history.isNotEmpty()),
            enter = expandVertically(tween(250)) + fadeIn(tween(250)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(200)),
            modifier = Modifier
                .fillMaxWidth()
                // 40%-screen cap: history is a quick pick list, not a page.
                // Overflow scrolls inside instead of covering the grid.
                .heightIn(max = screenHeightDp * 0.4f)
                .zIndex(1f)
                .offset { IntOffset(0, barHeight) }
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, 0) {
                        placeable.place(0, 0)
                    }
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, PanelShape)
                    .clip(PanelShape)
                    .background(Color.Black.copy(alpha = 0.55f), PanelShape)
                    .background(Color.White.copy(alpha = 0.35f), PanelShape)
                    .background(Color(0xFF3A3A3C).copy(alpha = 0.85f), PanelShape)
                    .background(Color(0xFF2C2C2E).copy(alpha = 0.65f), PanelShape)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = KraftSpacing.Spacing16, vertical = KraftSpacing.Spacing8),
            ) {
                if (trimmedQuery.isEmpty()) {
                    if (history.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            FilterSectionLabel(stringResource(R.string.search_recent))
                            TextButton(onClick = onClearHistory) {
                                Text(
                                    text = stringResource(R.string.search_clear_history),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                        history.take(10).forEach { item ->
                            SuggestionRow(
                                text = item,
                                icon = Icons.Filled.History,
                                onClick = { selectSuggestion(item) },
                            )
                        }
                    }
                } else {
                    typedMatches.forEach { match ->
                        SuggestionRow(text = match, icon = null, onClick = { selectSuggestion(match) })
                    }
                }
            }
        }
    }
}

@Composable
fun FilterSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Single suggestion row: optional leading icon + text, 44dp touch target. */
@Composable
private fun SuggestionRow(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = KraftSpacing.TouchTarget)
            .clip(RoundedCornerShape(KraftRadius.Standard))
            .clickable(onClick = onClick)
            .padding(horizontal = KraftSpacing.Spacing8, vertical = KraftSpacing.Spacing8),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(KraftIconSize.Small),
            )
            Spacer(Modifier.width(KraftSpacing.Spacing12))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
