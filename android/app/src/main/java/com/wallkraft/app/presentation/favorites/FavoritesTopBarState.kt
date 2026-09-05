package com.wallkraft.app.presentation.favorites

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Shared top-bar state between the outer Scaffold (KraftTopBar) and
 * FavoritesScreen content. Lives outside the SharedTransitionLayout so the
 * top bar is never eclipsed by the shared element overlay.
 *
 * FavoritesScreen writes to [selectionMode], [selectedCount], and the
 * callback lambdas on every recomposition. The outer Scaffold reads them
 * to render the correct KraftTopBar (title, navigation icon, actions).
 */
class FavoritesTopBarState {
    var selectionMode by mutableStateOf(false)
    var selectedCount by mutableStateOf(0)
    var totalFavorites by mutableStateOf(0)
    /** Close button — clears selection. */
    var onCancelSelection by mutableStateOf<() -> Unit>({})
    /** Select All / Deselect All toggle. */
    var onToggleSelectAll by mutableStateOf<() -> Unit>({})
    /** Delete selected — opens the confirmation dialog. */
    var onDeleteSelected by mutableStateOf<() -> Unit>({})
    /** Enters selection mode with all items selected. */
    var onEnterSelectionMode by mutableStateOf<() -> Unit>({})
}
