package com.wallkraft.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftTopBar
import com.wallkraft.app.core.utils.KraftHaptics
import com.wallkraft.app.core.utils.rememberReduceMotion
import com.wallkraft.app.presentation.favorites.FavoritesTopBarState

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FavoritesSelectionTopBar(
    topBarState: FavoritesTopBarState,
) {
    val reduceMotion = rememberReduceMotion()
    val title = if (topBarState.selectionMode) {
        pluralStringResource(
            R.plurals.selected_count,
            topBarState.selectedCount,
            topBarState.selectedCount,
        )
    } else {
        stringResource(R.string.favorites_title)
    }
    KraftTopBar(
        title = title,
        navigationIcon = {
            AnimatedVisibility(
                visible = topBarState.selectionMode,
                enter = if (reduceMotion) fadeIn(tween(220))
                else slideInVertically(tween(220)) { -it / 2 } + fadeIn(tween(220)),
                exit = if (reduceMotion) fadeOut(tween(180))
                else slideOutVertically(tween(180)) { -it / 2 } + fadeOut(tween(180)),
            ) {
                IconButton(onClick = { topBarState.onCancelSelection() }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.cancel),
                    )
                }
            }
        },
        actions = {
            AnimatedVisibility(
                visible = topBarState.selectionMode,
                enter = if (reduceMotion) fadeIn(tween(220))
                else fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.8f),
                exit = if (reduceMotion) fadeOut(tween(180))
                else fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.8f),
            ) {
                Row {
                    val haptic = LocalHapticFeedback.current
                    TextButton(
                        onClick = {
                            KraftHaptics.buttonPress(haptic)
                            topBarState.onToggleSelectAll()
                        },
                    ) {
                        Text(
                            stringResource(
                                if (topBarState.allVisibleSelected) R.string.deselect_all
                                else R.string.select_all,
                            ),
                        )
                    }
                    IconButton(
                        onClick = {
                            KraftHaptics.buttonPress(haptic)
                            topBarState.onAddToCollection()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CreateNewFolder,
                            contentDescription = stringResource(R.string.add_to_collection),
                        )
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (topBarState.isInCollection) {
                                topBarState.onRemoveFromCollection?.invoke()
                            } else {
                                topBarState.onDeleteSelected()
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = if (topBarState.isInCollection) {
                                stringResource(R.string.remove_from_collection)
                            } else {
                                stringResource(R.string.delete)
                            },
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
    )
}
