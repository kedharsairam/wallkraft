package com.wallkraft.app.presentation.favorites

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wallkraft.app.AppContainer
import com.wallkraft.app.R
import com.wallkraft.app.domain.model.Wallpaper
import androidx.compose.animation.ExperimentalSharedTransitionApi
import com.wallkraft.app.presentation.components.EmptyState
import com.wallkraft.app.presentation.components.WallpaperGrid
import com.wallkraft.app.domain.model.DownloadedFile
import com.wallkraft.app.util.WallpaperActions

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FavoritesScreen(
    container: AppContainer,
    onOpenWallpaper: (Wallpaper) -> Unit,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
    navBarPadding: Dp = 0.dp,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
    topBarState: FavoritesTopBarState = FavoritesTopBarState(),
) {
    val viewModel: FavoritesViewModel = viewModel(
        factory = viewModelFactory {
            initializer { FavoritesViewModel(container.favoritesRepository) }
        },
    )
    val favorites by viewModel.favorites.collectAsState()
    // The set of downloaded IDs, so the grid can badge cards that are already
    // on disk. Refreshed on resume — a download from the detail screen must
    // show up without restarting the app.
    var downloadedFiles by remember { mutableStateOf(emptyMap<String, DownloadedFile>()) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // Data saver: skip the full-res prefetch on tap (favorites are already
    // local files, so the detail screen loads them instantly anyway).
    var prefetchFullRes by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        prefetchFullRes = !container.settings.current().dataSaverMode
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                downloadedFiles = WallpaperActions.downloadedFiles(context)
                    .associateBy { it.wallpaperId }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Selection mode state — derived from selectedIds to prevent desync
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    val selectionMode = selectedIds.isNotEmpty()
    var pendingRemove by remember { mutableStateOf<List<Wallpaper>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Sync shared top bar state (lives outside SharedTransitionLayout).
    val allSelected = favorites.isNotEmpty() && selectedIds.size == favorites.size
    topBarState.selectionMode = selectionMode
    topBarState.selectedCount = selectedIds.size
    topBarState.totalFavorites = favorites.size
    topBarState.onCancelSelection = { selectedIds = emptySet() }
    topBarState.onToggleSelectAll = {
        selectedIds = if (allSelected) {
            emptySet()
        } else {
            favorites.mapTo(mutableSetOf()) { it.wallpaper.id }
        }
    }
    topBarState.onDeleteSelected = {
        pendingRemove = favorites
            .filter { it.wallpaper.id in selectedIds }
            .map { it.wallpaper }
    }
    topBarState.onEnterSelectionMode = {
        selectedIds = favorites.mapTo(mutableSetOf()) { it.wallpaper.id }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (favorites.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.no_favorites_title),
                message = stringResource(R.string.no_favorites_message),
                icon = Icons.Outlined.FavoriteBorder,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            WallpaperGrid(
                wallpapers = favorites.map { it.wallpaper },
                onOpen = { wallpaper ->
                    if (selectionMode) {
                        selectedIds = if (wallpaper.id in selectedIds) {
                            selectedIds - wallpaper.id
                        } else {
                            selectedIds + wallpaper.id
                        }
                    } else {
                        onOpenWallpaper(wallpaper)
                    }
                },
                onLoadMore = {},
                state = gridState,
                downloadedIds = downloadedFiles.keys,
                prefetchFullRes = prefetchFullRes,
                onLongClick = { wallpaper ->
                    selectedIds = setOf(wallpaper.id)
                },
                selectionMode = selectionMode,
                selectedIds = selectedIds,
                onToggleSelect = { wallpaper ->
                    selectedIds = if (wallpaper.id in selectedIds) {
                        selectedIds - wallpaper.id
                    } else {
                        selectedIds + wallpaper.id
                    }
                },
                modifier = Modifier.padding(innerPadding),
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    }

    // Remove confirmation dialog
    pendingRemove?.let { wallpapersToRemove ->
        val count = wallpapersToRemove.size
        AlertDialog(
            onDismissRequest = { pendingRemove = null },
            title = {
                Text(
                    if (count == 1) {
                        stringResource(R.string.remove_favorites_confirm_title)
                    } else {
                        pluralStringResource(
                            R.plurals.remove_favorites_selected_title,
                            count,
                            count,
                        )
                    },
                )
            },
            text = {
                Text(
                    if (count == 1) {
                        stringResource(R.string.remove_favorites_confirm_message)
                    } else {
                        pluralStringResource(
                            R.plurals.remove_favorites_selected_message,
                            count,
                            count,
                        )
                    },
                )
            },
            confirmButton = {
                val haptic = LocalHapticFeedback.current
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        wallpapersToRemove.forEach { viewModel.remove(it.id) }
                        val removedIds = wallpapersToRemove.mapTo(mutableSetOf()) { it.id }
                        selectedIds = selectedIds - removedIds
                        pendingRemove = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemove = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
