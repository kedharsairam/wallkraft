package com.wallkraft.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.imageLoader
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.util.DownloadedFile
import java.util.Locale

/**
 * List-style view of downloaded wallpapers (the Downloads tab). Each row shows
 * the thumbnail (loaded straight from the local file), the file's real
 * name/size/location, a folder button that reveals the file in the system
 * Files app, and a delete button.
 *
 * Tapping the row opens the wallpaper in the app; the folder and delete
 * buttons are the only elements that leave the app or change files.
 *
 * In selection mode rows show a check indicator instead of the action
 * buttons, and tapping a row toggles its selection. The screen owns the
 * selection state and drives batch actions (e.g. delete) from it.
 */
@Composable
fun DownloadedList(
    files: List<DownloadedFile>,
    onOpen: (DownloadedFile) -> Unit,
    onOpenLocation: (DownloadedFile) -> Unit,
    onDelete: (DownloadedFile) -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onToggleSelect: (DownloadedFile) -> Unit = {},
) {
    val context = LocalContext.current
    val gridImageLoader = GridImageLoader.get() ?: context.imageLoader

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = KraftSpacing.ScreenEdge,
            end = KraftSpacing.ScreenEdge,
            top = KraftSpacing.Spacing8,
            bottom = KraftSpacing.Spacing8,
        ),
        verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
    ) {
        items(files, key = { it.wallpaperId }) { file ->
            DownloadedRow(
                file = file,
                imageLoader = gridImageLoader,
                onClick = {
                    if (selectionMode) onToggleSelect(file) else onOpen(file)
                },
                onOpenLocation = { onOpenLocation(file) },
                onDelete = { onDelete(file) },
                selectionMode = selectionMode,
                selected = file.wallpaperId in selectedIds,
            )
        }
    }
}

@Composable
private fun DownloadedRow(
    file: DownloadedFile,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    onOpenLocation: () -> Unit,
    onDelete: () -> Unit,
    selectionMode: Boolean,
    selected: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KraftRadius.Standard))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
            )
            .clickable { onClick() }
            .padding(KraftSpacing.Spacing8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            Icon(
                imageVector = if (selected) {
                    Icons.Filled.CheckCircle
                } else {
                    Icons.Outlined.RadioButtonUnchecked
                },
                contentDescription = if (selected) {
                    stringResource(R.string.selected)
                } else {
                    null
                },
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(KraftSpacing.Spacing12))
        }
        AsyncImage(
            model = file.uri,
            contentDescription = file.name,
            contentScale = ContentScale.Crop,
            imageLoader = imageLoader,
            placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
            error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(KraftRadius.Small)),
        )

        Spacer(Modifier.width(KraftSpacing.Spacing12))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatSize(file.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = file.relativePath,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!selectionMode) {
            IconButton(onClick = onOpenLocation) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = stringResource(R.string.open_location),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** Formats a byte count as a compact human-readable size (e.g. "7.3 MB"). */
private fun formatSize(bytes: Long): String =
    if (bytes < 1024 * 1024) {
        String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    } else {
        String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }