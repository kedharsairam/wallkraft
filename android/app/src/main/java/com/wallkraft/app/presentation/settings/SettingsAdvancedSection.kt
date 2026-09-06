package com.wallkraft.app.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftColors
import com.wallkraft.app.core.design.KraftIconSize
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing

/**
 * Advanced section — Wallhaven API key row with validation status.
 *
 * Split from the former SettingsScreen god object. Reads state, reports
 * the tap through a callback; the screen owns the dialog.
 */
@Composable
fun SettingsAdvancedSection(
    apiKeyText: String,
    isValidating: Boolean,
    apiKeyValid: Boolean,
    onApiClick: () -> Unit,
) {
    SettingsGroup(title = stringResource(R.string.advanced_title)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KraftRadius.Standard))
                .clickable { onApiClick() }
                .padding(vertical = KraftSpacing.Spacing8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.api_key_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                val statusText = when {
                    apiKeyText.isBlank() -> stringResource(R.string.api_key_not_set)
                    isValidating -> stringResource(R.string.api_key_verifying)
                    apiKeyValid -> stringResource(R.string.api_key_valid)
                    else -> stringResource(R.string.api_key_invalid)
                }
                val statusColor = when {
                    apiKeyText.isBlank() -> MaterialTheme.colorScheme.onSurfaceVariant
                    isValidating -> MaterialTheme.colorScheme.onSurfaceVariant
                    apiKeyValid -> KraftColors.AuroraGreen
                    else -> KraftColors.AuroraRed
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(KraftIconSize.Small),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
