package com.wallkraft.app.presentation.favorites

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.wallkraft.app.core.design.KraftTopBar
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val title = if (selectionMode) {
                pluralStringResource(R.plurals.selected_count, selectedIds.size, selectedIds.size)
            } else {
                stringResource(R.string.favorites_title)
            }
            KraftTopBar(
                title = title,
                navigationIcon = if (selectionMode) {
                    {
                        IconButton(onClick = {
                            selectedIds = emptySet()
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.cancel),
                            )
                        }
                    }
                } else null,
                actions = {
                    AnimatedVisibility(
                        visible = selectionMode,
                        enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.8f),
                        exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.8f),
                    ) {
                        Row {
                            val allSelected = favorites.isNotEmpty() &&
                                selectedIds.size == favorites.size
                            TextButton(
                                onClick = {
                                    selectedIds = if (allSelected) {
                                        emptySet()
                                    } else {
                                        favorites.mapTo(mutableSetOf()) { it.wallpaper.id }
                                    }
                                },
                            ) {
                                Text(
                                    stringResource(
                                        if (allSelected) R.string.deselect_all else R.string.select_all,
                                    ),
                                )
                            }
                            IconButton(
                                onClick = {
                                    pendingRemove = favorites
                                        .filter { it.wallpaper.id in selectedIds }
                                        .map { it.wallpaper }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = !selectionMode && favorites.isNotEmpty(),
                        enter = fadeIn(tween(220)),
                        exit = fadeOut(tween(180)),
                    ) {
                        TextButton(onClick = {
                            selectedIds = favorites.mapTo(mutableSetOf()) { it.wallpaper.id }
                        }) {
                            Text(stringResource(R.string.select))
                        }
                    }
                },
            )
        },
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
                TextButton(
                    onClick = {
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
