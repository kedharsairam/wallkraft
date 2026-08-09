package com.wallkraft.app.presentation.downloads

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wallkraft.app.R
import com.wallkraft.app.domain.model.Thumbs
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.presentation.components.DownloadedList
import com.wallkraft.app.presentation.components.EmptyState
import com.wallkraft.app.util.DownloadedFile
import com.wallkraft.app.util.WallpaperActions

/**
 * The Downloads tab: every wallpaper file the app has saved to the public
 * Downloads folder, as a list of rows (thumbnail, file info, open-location
 * and delete actions). The file list is re-scanned every time the screen
 * resumes, so a download from the detail screen — or a delete here — is
 * reflected without restarting the app.
 *
 * Selection mode lets the user pick several files (or all of them) and
 * delete them in one confirmation, instead of confirming each file.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onOpenWallpaper: (Wallpaper) -> Unit,
    navBarPadding: Dp = 0.dp,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var files by remember { mutableStateOf(emptyList<DownloadedFile>()) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var isSelecting by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<List<DownloadedFile>?>(null) }

    val selectionMode = isSelecting

    // Load immediately on first composition — a fresh navigation entry is
    // already RESUMED when the observer below registers, so ON_RESUME alone
    // would miss it. The observer then keeps the list fresh on later resumes.
    LaunchedEffect(Unit) {
        files = WallpaperActions.downloadedFiles(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                files = WallpaperActions.downloadedFiles(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Drop selections that no longer exist on disk (e.g. a file was deleted
    // from the Files app while we were away). Keeps the count honest.
    LaunchedEffect(files) {
        val valid = files.mapTo(mutableSetOf()) { it.wallpaperId }
        if (selectedIds.any { it !in valid }) {
            selectedIds = selectedIds.intersect(valid)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectionMode) {
                            stringResource(R.string.selected_count, selectedIds.size)
                        } else {
                            stringResource(R.string.downloads_title)
                        },
                    )
                },
                navigationIcon = {
                    if (selectionMode) {
                        IconButton(onClick = {
                            selectedIds = emptySet()
                            isSelecting = false
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.cancel),
                            )
                        }
                    }
                },
                actions = {
                    if (selectionMode) {
                        val allSelected = files.isNotEmpty() && selectedIds.size == files.size
                        TextButton(
                            onClick = {
                                selectedIds = if (allSelected) {
                                    emptySet()
                                } else {
                                    files.mapTo(mutableSetOf()) { it.wallpaperId }
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
                                pendingDelete = files.filter { it.wallpaperId in selectedIds }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    } else if (files.isNotEmpty()) {
                        TextButton(onClick = { isSelecting = true }) {
                            Text(stringResource(R.string.select))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (files.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.no_downloads_title),
                message = stringResource(R.string.no_downloads_message),
                icon = Icons.Outlined.Download,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
        } else {
            DownloadedList(
                files = files,
                onOpen = { file -> onOpenWallpaper(file.toWallpaper()) },
                onOpenLocation = { file -> WallpaperActions.openDownloadLocation(context, file) },
                onDelete = { file -> pendingDelete = listOf(file) },
                selectionMode = selectionMode,
                selectedIds = selectedIds,
                onToggleSelect = { file ->
                    selectedIds = if (file.wallpaperId in selectedIds) {
                        selectedIds - file.wallpaperId
                    } else {
                        selectedIds + file.wallpaperId
                    }
                },
                modifier = Modifier.padding(innerPadding).padding(bottom = navBarPadding),
            )
        }
    }

    pendingDelete?.let { filesToDelete ->
        val count = filesToDelete.size
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = {
                Text(
                    if (count == 1) {
                        stringResource(R.string.delete_confirm_title)
                    } else {
                        stringResource(R.string.delete_selected_title, count)
                    },
                )
            },
            text = {
                Text(
                    if (count == 1) {
                        stringResource(R.string.delete_confirm_message)
                    } else {
                        stringResource(R.string.delete_selected_message, count)
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        filesToDelete.forEach { WallpaperActions.delete(context, it) }
                        val deletedIds = filesToDelete.mapTo(mutableSetOf()) { it.wallpaperId }
                        files = files.filterNot { it.wallpaperId in deletedIds }
                        selectedIds = selectedIds - deletedIds
                        if (files.isEmpty()) isSelecting = false
                        pendingDelete = null
                    },
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

/**
 * Builds a minimal [Wallpaper] from a downloaded file so the detail screen can
 * render it instantly from the local file (via [DownloadedFile.uri]) while the
 * API metadata loads in the background.
 */
private fun DownloadedFile.toWallpaper(): Wallpaper =
    Wallpaper(
        id = wallpaperId,
        path = uri.toString(),
        thumbs = Thumbs(original = uri.toString()),
    )