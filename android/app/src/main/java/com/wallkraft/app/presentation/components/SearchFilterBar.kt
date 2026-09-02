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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    var isFocused by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var barHeight by remember { mutableIntStateOf(0) }

    // Draft filters — staged inside the panel, only committed on Apply.
    var draftFilters by remember { mutableStateOf(filters) }
    androidx.compose.runtime.LaunchedEffect(showFilters) {
        if (showFilters) draftFilters = filters
    }

    Box(modifier = modifier) {
        // ── Main content ────────────────────────────────────────────────
        Column(
            modifier = Modifier.onGloballyPositioned { coordinates ->
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
                        .height(KraftSpacing.TouchTarget)
                        .onFocusChanged { isFocused = it.isFocused },
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
.height(KraftSpacing.TouchTarget)
                                .clip(PillShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 14.dp),
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
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
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
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = KraftConstants.DividerAlpha),
            )
        }

        // ── Filter panel (drops down from below the bar) ────────────────
        // Use layout {} to report zero size so the parent Box doesn't grow.
        AnimatedVisibility(
            visible = showFilters,
            enter = expandVertically(tween(250)) + fadeIn(tween(250)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(200)),
            modifier = Modifier
                .fillMaxWidth()
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, PanelShape)
                    .clip(PanelShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = KraftSpacing.Spacing16, vertical = KraftSpacing.Spacing16),
            ) {
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
                        )
                    }
                }
                Spacer(Modifier.height(KraftSpacing.Spacing12))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = KraftConstants.OutlineVariantAlpha))
                Spacer(Modifier.height(KraftSpacing.Spacing12))

                FilterSectionLabel(stringResource(R.string.filter_purity))
                Spacer(Modifier.height(KraftSpacing.Spacing8))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Purity.entries.forEach { p ->
                        val checked = p in draftFilters.purity
                        FilterChip(
                            selected = checked,
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                val current = draftFilters.purity
                                val updated = if (p in current) {
                                    if (current.size > 1) current - p else current
                                } else current + p
                                draftFilters = draftFilters.copy(purity = updated)
                            },
                            label = { Text(p.displayName()) },
                        )
                    }
                }
                Spacer(Modifier.height(KraftSpacing.Spacing12))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = KraftConstants.OutlineVariantAlpha))
                Spacer(Modifier.height(KraftSpacing.Spacing12))

                FilterSectionLabel(stringResource(R.string.filter_sorting))
                Spacer(Modifier.height(KraftSpacing.Spacing8))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
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
                        )
                    }
                }
                Spacer(Modifier.height(KraftSpacing.Spacing12))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = KraftConstants.OutlineVariantAlpha))
                Spacer(Modifier.height(KraftSpacing.Spacing12))

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
                        )
                    }
                }
                Spacer(Modifier.height(KraftSpacing.Spacing12))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = KraftConstants.OutlineVariantAlpha))
                Spacer(Modifier.height(KraftSpacing.Spacing12))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing12),
                ) {
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            draftFilters = WallhavenFilters(query = filters.query)
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.filter_reset), color = MaterialTheme.colorScheme.error) }
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            if (draftFilters != filters) onFiltersChange(draftFilters)
                            showFilters = false
                        },
                        enabled = draftFilters != filters,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.filter_apply)) }
                }
                Spacer(Modifier.height(KraftSpacing.Spacing8))
            }
        }
    }
}

@Composable
private fun FilterSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = KraftTypeScale.Footnote),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
