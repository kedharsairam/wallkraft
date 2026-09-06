package com.wallkraft.app.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftSpacing

/**
 * Data section — data saver toggle and cache management.
 *
 * Split from the former SettingsScreen god object. Reads state, reports
 * changes through callbacks; the screen owns the ViewModel and dialogs.
 */
@Composable
fun SettingsDataSection(
    dataSaverMode: Boolean,
    onDataSaverChange: (Boolean) -> Unit,
    cacheSizeText: String,
    onClearCacheClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    SettingsGroup(title = stringResource(R.string.data_title)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.data_saver_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.data_saver_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = KraftSpacing.Spacing2),
                )
            }
            Switch(
                checked = dataSaverMode,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDataSaverChange(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = KraftSpacing.Spacing12),
            color = MaterialTheme.colorScheme.outline,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cache_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "${stringResource(R.string.cache_description)} • $cacheSizeText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClearCacheClick()
            }) {
                Text(stringResource(R.string.clear_cache))
            }
        }
    }
}
