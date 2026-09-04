package com.wallkraft.app.presentation.components

import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.wallkraft.app.core.design.KraftColors

/** Non-purity chip colors (categories, sorting, orientation). */
@Composable
fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = KraftColors.ChipSelectedContainer,
    selectedLabelColor = KraftColors.ChipSelectedLabel,
)

/** Purity SFW chip colors. */
@Composable
fun puritySfwChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = KraftColors.PuritySfwContainer,
    selectedLabelColor = KraftColors.PuritySfwLabel,
)

/** Purity Sketchy chip colors. */
@Composable
fun puritySketchyChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = KraftColors.PuritySketchyContainer,
    selectedLabelColor = KraftColors.PuritySketchyLabel,
)

/** Purity NSFW chip colors. */
@Composable
fun purityNsfwChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = KraftColors.PurityNsfwContainer,
    selectedLabelColor = KraftColors.PurityNsfwLabel,
)
