package com.wallkraft.app.presentation.downloads

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.wallkraft.app.R
import com.wallkraft.app.domain.model.Thumbs
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.presentation.components.DownloadedList
import com.wallkraft.app.presentation.components.EmptyState
import com.wallkraft.app.util.DownloadedFile
import com.wallkraft.app.util.WallpaperActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
@Composable
fun DownloadsScreen(
    onOpenWallpaper: (Wallpaper) -> Unit,
    navBarPadding: Dp = 0.dp,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var files by remember { mutableStateOf(emptyList<DownloadedFile>()) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    val selectionMode = selectedIds.isNotEmpty()
    var pendingDelete by remember { mutableStateOf<List<DownloadedFile>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

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
    // from the Files app while we were away). Keeps the count honest and
    // informs the user instead of silently changing the number.
    LaunchedEffect(files) {
        val valid = files.mapTo(mutableSetOf()) { it.wallpaperId }
        val removed = selectedIds.count { it !in valid }
        if (removed > 0) {
            selectedIds = selectedIds.intersect(valid)
            snackbarHostState.showSnackbar(
                context.resources.getQuantityString(
                    R.plurals.files_removed_outside_app,
                    removed,
                    removed,
                ),
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val title = if (selectionMode) {
                pluralStringResource(R.plurals.selected_count, selectedIds.size, selectedIds.size)
            } else {
                stringResource(R.string.downloads_title)
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
                        TextButton(onClick = {
                            selectedIds = files.mapTo(mutableSetOf()) { it.wallpaperId }
                        }) {
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
                modifier = Modifier.fillMaxSize().padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding(),
                ),
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
                onEnterSelection = { file ->
                    selectedIds = setOf(file.wallpaperId)
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
                        pluralStringResource(R.plurals.delete_selected_title, count, count)
                    },
                )
            },
            text = {
                Text(
                    if (count == 1) {
                        stringResource(R.string.delete_confirm_message)
                    } else {
                        pluralStringResource(R.plurals.delete_selected_message, count, count)
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                filesToDelete.forEach { WallpaperActions.delete(context, it) }
                            }
                            val deletedIds = filesToDelete.mapTo(mutableSetOf()) { it.wallpaperId }
                            files = files.filterNot { it.wallpaperId in deletedIds }
                            selectedIds = selectedIds - deletedIds
                            pendingDelete = null
                        }
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