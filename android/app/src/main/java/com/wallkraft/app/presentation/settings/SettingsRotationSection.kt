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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftIconSize
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.data.db.CollectionWithItems
import com.wallkraft.app.data.prefs.RotationSettings
import com.wallkraft.app.domain.model.RotationMode
import com.wallkraft.app.domain.model.RotationSchedule
import com.wallkraft.app.domain.model.RotationTarget
import com.wallkraft.app.presentation.components.chipColors
import com.wallkraft.app.presentation.components.FilterSectionLabel

/**
 * Wallpaper rotation section — schedule, style, target screen, source, and
 * a manual trigger. Fully offline: rotates through local favorite files.
 *
 * Split from SettingsScreen assembly; the screen owns persistence (store)
 * and scheduling (WorkManager) via callbacks.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsRotationSection(
    settings: RotationSettings,
    collections: List<CollectionWithItems>,
    onSchedule: (RotationSchedule) -> Unit,
    onMode: (RotationMode) -> Unit,
    onTarget: (RotationTarget) -> Unit,
    onSource: (Long?) -> Unit,
    onRotateNow: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var showSource by remember { mutableStateOf(false) }
    val sourceName = collections.firstOrNull { it.collection.id == settings.sourceCollectionId }
        ?.collection?.name ?: stringResource(R.string.rotation_all_favorites)

    SettingsGroup(title = stringResource(R.string.rotation_title)) {
        var expanded by remember { mutableStateOf(false) }
        val scheduleLabel = rotationScheduleOptions().firstOrNull { it.first == settings.schedule }?.second.orEmpty()
        val modeLabel = rotationModeOptions().firstOrNull { it.first == settings.mode }?.second.orEmpty()
        val targetLabel = rotationTargetOptions().firstOrNull { it.first == settings.target }?.second.orEmpty()

        // Summary row — tap to expand/collapse (same pattern as Default Filters).
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
                Text(
                    text = "$scheduleLabel • $modeLabel • $targetLabel • $sourceName",
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

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring(dampingRatio = 0.7f, stiffness = 400f)) + fadeIn(),
            exit = shrinkVertically(spring(dampingRatio = 0.7f, stiffness = 400f)) + fadeOut(),
        ) {
            Column {
        // Schedule
        FilterSectionLabel(stringResource(R.string.rotation_schedule))
                Spacer(Modifier.height(KraftSpacing.Spacing4))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing6),
            verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing4),
            modifier = Modifier.fillMaxWidth(),
        ) {
            rotationScheduleOptions().forEach { (option, label) ->
                FilterChip(
                    selected = settings.schedule == option,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSchedule(option)
                    },
                    label = { Text(label) },
                    colors = chipColors(),
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = KraftSpacing.Spacing4),
            color = MaterialTheme.colorScheme.outline,
        )
        // Style
        FilterSectionLabel(stringResource(R.string.rotation_style))
                Spacer(Modifier.height(KraftSpacing.Spacing4))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing6),
            verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing4),
            modifier = Modifier.fillMaxWidth(),
        ) {
            rotationModeOptions().forEach { (option, label) ->
                FilterChip(
                    selected = settings.mode == option,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMode(option)
                    },
                    label = { Text(label) },
                    colors = chipColors(),
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = KraftSpacing.Spacing4),
            color = MaterialTheme.colorScheme.outline,
        )
        // Target screen
        FilterSectionLabel(stringResource(R.string.rotation_screen))
                Spacer(Modifier.height(KraftSpacing.Spacing4))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing6),
            verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing4),
            modifier = Modifier.fillMaxWidth(),
        ) {
            rotationTargetOptions().forEach { (option, label) ->
                FilterChip(
                    selected = settings.target == option,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTarget(option)
                    },
                    label = { Text(label) },
                    colors = chipColors(),
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = KraftSpacing.Spacing4),
            color = MaterialTheme.colorScheme.outline,
        )
        // Source
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KraftRadius.Standard))
                .clickable { showSource = true }
                .padding(vertical = KraftSpacing.Spacing8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.rotation_source),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = sourceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(KraftIconSize.Small),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onRotateNow()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(KraftSpacing.TouchTarget)
                .padding(top = KraftSpacing.Spacing4),
        ) {
            Text(stringResource(R.string.rotation_now))
        }
            }
        }
    }

    if (showSource) {
        AlertDialog(
            onDismissRequest = { showSource = false },
            title = { Text(stringResource(R.string.rotation_source)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    item(key = "all") {
                        SourceRow(
                            name = stringResource(R.string.rotation_all_favorites),
                            selected = settings.sourceCollectionId == null,
                            onClick = {
                                onSource(null)
                                showSource = false
                            },
                        )
                    }
                    items(collections, key = { "source-${it.collection.id}" }) { entry ->
                        SourceRow(
                            name = entry.collection.name,
                            selected = entry.collection.id == settings.sourceCollectionId,
                            onClick = {
                                onSource(entry.collection.id)
                                showSource = false
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSource = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun rotationScheduleOptions(): List<Pair<RotationSchedule, String>> = listOf(
    RotationSchedule.OFF to stringResource(R.string.rotation_off),
    RotationSchedule.DAILY to stringResource(R.string.rotation_daily),
    RotationSchedule.WEEKLY to stringResource(R.string.rotation_weekly),
)

@Composable
private fun rotationModeOptions(): List<Pair<RotationMode, String>> = listOf(
    RotationMode.SHOWCASE to stringResource(R.string.rotation_showcase),
    RotationMode.ATMOSPHERE to stringResource(R.string.rotation_atmosphere),
    RotationMode.FILL to stringResource(R.string.rotation_fill),
)

@Composable
private fun rotationTargetOptions(): List<Pair<RotationTarget, String>> = listOf(
    RotationTarget.HOME to stringResource(R.string.wallpaper_position_home),
    RotationTarget.LOCK to stringResource(R.string.wallpaper_position_lock),
    RotationTarget.BOTH to stringResource(R.string.wallpaper_position_both),
)

@Composable
private fun SourceRow(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KraftRadius.Standard))
            .clickable(onClick = onClick)
            .padding(vertical = KraftSpacing.Spacing8),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = KraftSpacing.Spacing12),
        )
    }
}
