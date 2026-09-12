package com.wallkraft.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.domain.model.AppUpdateInfo

/**
 * Update available sheet — below top bar pattern, Hero 20dp + 36x6 pill.
 * Shows version + size + first 200 chars of notes. Download / Later.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateAvailableDialog(
    info: AppUpdateInfo,
    downloadState: SettingsViewModel.DownloadUiState = SettingsViewModel.DownloadUiState.Idle,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = KraftRadius.Hero, topEnd = KraftRadius.Hero),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(KraftSpacing.Spacing20).padding(bottom = KraftSpacing.Spacing12)) {
            Text(
                text = stringResource(R.string.update_available_title) + " v${info.version} (${formatBytes(info.sizeBytes)})",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(KraftSpacing.Spacing12))
            if (info.notes.isNotBlank()) {
                Text(
                    text = info.notes.take(200),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(KraftSpacing.Spacing12))
            }
            when (val d = downloadState) {
                is SettingsViewModel.DownloadUiState.Downloading -> {
                    val total = if (d.total > 0) d.total else info.sizeBytes
                    val pct = if (total > 0) (d.read * 100 / total).toInt().coerceIn(0, 100) else 0
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { pct / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(KraftSpacing.Spacing8))
                    Text(
                        text = "${formatBytes(d.read)} / ${formatBytes(total)} ($pct%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is SettingsViewModel.DownloadUiState.Error -> {
                    Text(
                        text = d.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(KraftSpacing.Spacing8))
                }
                else -> Unit
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.later)) }
                Spacer(Modifier.width(KraftSpacing.Spacing8))
                val downloading = downloadState is SettingsViewModel.DownloadUiState.Downloading
                TextButton(onClick = onDownload, enabled = !downloading) {
                    if (downloading) {
                        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(KraftSpacing.Spacing8))
                    }
                    Text(stringResource(R.string.download))
                }
            }
        }
    }
}
