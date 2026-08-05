package com.wallkraft.app.presentation.downloads

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import java.io.File
import java.util.Locale

data class DownloadEntry(
    val id: Long,
    val uri: Uri,
    val title: String,
    val size: Long,
    val localPath: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    container: com.wallkraft.app.AppContainer,
    navBarPadding: Dp = 0.dp,
) {
    val context = LocalContext.current
    var downloads by remember { mutableStateOf<List<DownloadEntry>>(emptyList()) }
    var showRemoveDialog by rememberSaveable { mutableStateOf(false) }
    var entryToRemove by remember { mutableStateOf<DownloadEntry?>(null) }

    // Refresh downloads every time the screen becomes visible (e.g. after
    // downloading a wallpaper in the Detail screen and returning here).
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                downloads = queryDownloads(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        downloads = queryDownloads(context) // initial load
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.downloads_title)) })
        },
    ) { innerPadding ->
        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.downloads_empty))
                    Spacer(Modifier.height(KraftSpacing.Spacing8))
                    Text(
                        stringResource(R.string.downloads_empty_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                state = rememberLazyListState(),
                contentPadding = PaddingValues(
                    start = KraftSpacing.Spacing16,
                    top = innerPadding.calculateTopPadding(),
                    end = KraftSpacing.Spacing16,
                    bottom = innerPadding.calculateBottomPadding() + navBarPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(downloads, key = { it.id }) { entry ->
                    DownloadItem(
                        entry = entry,
                        onOpen = { openFile(context, entry) },
                        onRemove = {
                            entryToRemove = entry
                            showRemoveDialog = true
                        },
                    )
                }
            }
        }
    }

    if (showRemoveDialog && entryToRemove != null) {
        val entry = entryToRemove!!
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text(stringResource(R.string.downloads_remove_confirm)) },
            text = { Text(entry.title) },
            confirmButton = {
                TextButton(onClick = {
                    removeDownload(context, entry)
                    downloads = downloads - entry
                    showRemoveDialog = false
                    entryToRemove = null
                }) {
                    Text(stringResource(R.string.downloads_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun DownloadItem(
    entry: DownloadEntry,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KraftSpacing.Spacing16, vertical = KraftSpacing.Spacing8),
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .aspectRatio(0.75f)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(KraftRadius.Standard)),
        ) {
            AsyncImage(
                model = entry.uri,
                contentDescription = entry.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
        Spacer(Modifier.height(KraftSpacing.Spacing12))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = KraftSpacing.Spacing12),
        ) {
            Text(entry.title, style = MaterialTheme.typography.bodyMedium)
            Text(
                formatSize(entry.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledIconButton(onClick = onOpen, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.OpenInBrowser, contentDescription = stringResource(R.string.downloads_open))
        }
        FilledIconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.downloads_remove))
        }
    }
}

private fun queryDownloads(context: Context): List<DownloadEntry> {
    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val query = DownloadManager.Query()
    val cursor = dm.query(query)
    val entries = mutableListOf<DownloadEntry>()
    cursor?.use {
        val idIdx = it.getColumnIndex(DownloadManager.COLUMN_ID)
        val localUriIdx = it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
        val titleIdx = it.getColumnIndex(DownloadManager.COLUMN_TITLE)
        val sizeIdx = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
        val localFileIdx = it.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME)

        while (it.moveToNext()) {
            val id = it.getLong(idIdx)
            val uriStr = it.getString(localUriIdx)
            val title = it.getString(titleIdx)
            val size = it.getLong(sizeIdx)
            val localPath = it.getString(localFileIdx)
            if (uriStr != null && localPath != null && localPath.contains("WallKraft-")) {
                entries.add(
                    DownloadEntry(
                        id = id,
                        uri = Uri.parse(uriStr),
                        title = title,
                        size = size,
                        localPath = localPath,
                    ),
                )
            }
        }
    }
    return entries
}

private fun removeDownload(context: Context, entry: DownloadEntry) {
    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    dm.remove(entry.id)
    try {
        File(entry.localPath).delete()
    } catch (_: Exception) {
    }
}

private fun openFile(context: Context, entry: DownloadEntry) {
    try {
        val uri = entry.uri
        val mime = context.contentResolver.getType(uri) ?: "image/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // No app can handle this file type — silently ignore.
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}
