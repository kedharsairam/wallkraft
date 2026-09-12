package com.wallkraft.app.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftIconSize
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.domain.model.AppSettings
import com.wallkraft.app.domain.model.Category
import com.wallkraft.app.domain.model.Orientation
import com.wallkraft.app.domain.model.Purity
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.domain.model.TopRange
import com.wallkraft.app.presentation.components.chipColors
import com.wallkraft.app.presentation.components.FilterSectionLabel
import com.wallkraft.app.presentation.components.purityNsfwChipColors
import com.wallkraft.app.presentation.components.puritySfwChipColors
import com.wallkraft.app.presentation.components.puritySketchyChipColors
import com.wallkraft.app.util.displayName

/**
 * Default Filters section — a single collapsible group showing a compact
 * summary when collapsed and all filter chips when expanded.
 *
 * Split from the former SettingsScreen god object. Reads [settings], reports
 * changes through callbacks; the screen owns the ViewModel.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsBrowsingSection(
    settings: AppSettings,
    onCategories: (Set<Category>) -> Unit,
    onPurity: (Set<Purity>) -> Unit,
    onSorting: (Sorting) -> Unit,
    onTopRange: (TopRange) -> Unit,
    onOrientation: (Orientation) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    SettingsGroup(title = stringResource(R.string.browsing_title)) {
        // Pre-resolve display names for summary text
        val catGeneral = stringResource(R.string.category_general)
        val catAnime = stringResource(R.string.category_anime)
        val catPeople = stringResource(R.string.category_people)
        val puritySfw = stringResource(R.string.purity_sfw)
        val puritySketchy = stringResource(R.string.purity_sketchy)
        val purityNsfw = stringResource(R.string.purity_nsfw)
        val sortDateAdded = stringResource(R.string.sorting_date_added)
        val sortHot = stringResource(R.string.sorting_hot)
        val sortRandom = stringResource(R.string.sorting_random)
        val sortViews = stringResource(R.string.sorting_views)
        val sortFavorites = stringResource(R.string.sorting_favorites)
        val sortRelevance = stringResource(R.string.sorting_relevance)
        val sortToplist = stringResource(R.string.sorting_toplist)
        val rangeDay = stringResource(R.string.top_range_day)
        val rangeWeek = stringResource(R.string.top_range_week)
        val rangeMonth = stringResource(R.string.top_range_month)
        val rangeSixMonths = stringResource(R.string.top_range_six_months)
        val rangeYear = stringResource(R.string.top_range_year)
        val orientBoth = stringResource(R.string.orientation_both)
        val orientPortrait = stringResource(R.string.orientation_portrait)
        val orientLandscape = stringResource(R.string.orientation_landscape)
        val allLabel = stringResource(R.string.filter_all)

        var expanded by rememberSaveable { mutableStateOf(false) }

        // Summary row — tap to expand/collapse
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KraftRadius.Standard))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    expanded = !expanded
                }
                .padding(vertical = KraftSpacing.Spacing8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Categories summary
                val catSummary = if (settings.categories.size == Category.entries.size) {
                    allLabel
                } else {
                    settings.categories.joinToString { cat ->
                        when (cat) {
                            Category.General -> catGeneral
                            Category.Anime -> catAnime
                            Category.People -> catPeople
                        }
                    }
                }
                // Purity summary
                val purSummary = if (settings.purity.size == Purity.entries.size) {
                    allLabel
                } else {
                    settings.purity.joinToString { p ->
                        when (p) {
                            Purity.SFW -> puritySfw
                            Purity.Sketchy -> puritySketchy
                            Purity.NSFW -> purityNsfw
                        }
                    }
                }
                // Sorting + Orientation (range appended for Toplist)
                val sortSummary = when (settings.sorting) {
                    Sorting.Relevance -> sortRelevance
                    Sorting.Random -> sortRandom
                    Sorting.DateAdded -> sortDateAdded
                    Sorting.Views -> sortViews
                    Sorting.Favorites -> sortFavorites
                    Sorting.Toplist -> sortToplist
                    Sorting.Hot -> sortHot
                }
                val rangeSummary = when (settings.topRange) {
                    TopRange.Day -> rangeDay
                    TopRange.Week -> rangeWeek
                    TopRange.Month -> rangeMonth
                    TopRange.SixMonths -> rangeSixMonths
                    TopRange.Year -> rangeYear
                }
                val orientSummary = when (settings.orientation) {
                    Orientation.Both -> orientBoth
                    Orientation.Portrait -> orientPortrait
                    Orientation.Landscape -> orientLandscape
                }
                Text(
                    text = if (settings.sorting == Sorting.Toplist) {
                        "$catSummary • $purSummary • $sortSummary • $rangeSummary • $orientSummary"
                    } else {
                        "$catSummary • $purSummary • $sortSummary • $orientSummary"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(KraftIconSize.Small),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Expanded filters
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring(dampingRatio = 0.7f, stiffness = 400f)) + fadeIn(),
            exit = shrinkVertically(spring(dampingRatio = 0.7f, stiffness = 400f)) + fadeOut(),
        ) {
            Column {
                // Categories
                FilterSectionLabel(stringResource(R.string.settings_categories))
                Spacer(Modifier.height(KraftSpacing.Spacing4))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing4),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Category.entries.forEach { category ->
                        val selected = category in settings.categories
                        FilterChip(
                            selected = selected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val newSet = if (selected) settings.categories - category else settings.categories + category
                                onCategories(newSet)
                            },
                            label = { Text(category.displayName()) },
                            colors = chipColors(),
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = KraftSpacing.Spacing4),
                    color = MaterialTheme.colorScheme.outline,
                )
                // Purity
                FilterSectionLabel(stringResource(R.string.settings_purity))
                Spacer(Modifier.height(KraftSpacing.Spacing4))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing4),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // NSFW shown always — locked when no valid API key.
                    val hasApiKey = settings.apiKeyValid
                    Purity.entries.forEach { purity ->
                        val isNsfwLocked = purity == Purity.NSFW && !hasApiKey
                        val selected = purity in settings.purity && !isNsfwLocked
                        val currentChipColors = when {
                            purity == Purity.SFW -> puritySfwChipColors()
                            purity == Purity.Sketchy -> puritySketchyChipColors()
                            isNsfwLocked -> FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            else -> purityNsfwChipColors()
                        }
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (isNsfwLocked) return@FilterChip
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val newSet = if (selected) settings.purity - purity else settings.purity + purity
                                onPurity(newSet)
                            },
                            label = { Text(purity.displayName()) },
                            leadingIcon = if (isNsfwLocked) {
                                { Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(KraftIconSize.Tiny)) }
                            } else null,
                            colors = currentChipColors,
                            enabled = !isNsfwLocked,
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = KraftSpacing.Spacing4),
                    color = MaterialTheme.colorScheme.outline,
                )
                // Orientation
                FilterSectionLabel(stringResource(R.string.settings_orientation))
                Spacer(Modifier.height(KraftSpacing.Spacing4))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing4),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Orientation.entries.forEach { orientation ->
                        FilterChip(
                            selected = settings.orientation == orientation,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOrientation(orientation)
                            },
                            label = { Text(orientation.displayName()) },
                            colors = chipColors(),
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = KraftSpacing.Spacing4),
                    color = MaterialTheme.colorScheme.outline,
                )
                // Sorting (wallhaven.cc order)
                FilterSectionLabel(stringResource(R.string.settings_sorting))
                Spacer(Modifier.height(KraftSpacing.Spacing4))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing4),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Sorting.entries.forEach { sorting ->
                        FilterChip(
                            selected = settings.sorting == sorting,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSorting(sorting)
                            },
                            label = { Text(sorting.displayName()) },
                            colors = chipColors(),
                        )
                    }
                }
                // Top range — only for Toplist (like wallhaven.cc)
                AnimatedVisibility(
                    visible = settings.sorting == Sorting.Toplist,
                    enter = expandVertically(spring(dampingRatio = 0.7f, stiffness = 400f)) + fadeIn(),
                    exit = shrinkVertically(spring(dampingRatio = 0.7f, stiffness = 400f)) + fadeOut(),
                ) {
                    Column {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = KraftSpacing.Spacing4),
                            color = MaterialTheme.colorScheme.outline,
                        )
                        FilterSectionLabel(stringResource(R.string.settings_top_range))
                        Spacer(Modifier.height(KraftSpacing.Spacing4))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                            verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing4),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            TopRange.entries.forEach { range ->
                                FilterChip(
                                    selected = settings.topRange == range,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onTopRange(range)
                                    },
                                    label = { Text(range.displayName()) },
                                    colors = chipColors(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
