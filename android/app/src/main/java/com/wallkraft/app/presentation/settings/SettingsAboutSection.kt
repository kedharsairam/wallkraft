package com.wallkraft.app.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wallkraft.app.BuildConfig
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftSpacing

/**
 * About section — developer credit, version, and link rows.
 *
 * Split from the former SettingsScreen god object. External links open
 * here; the privacy row reports through a callback (screen owns the dialog).
 */
@Composable
fun SettingsAboutSection(
    githubUrl: String,
    onPrivacyClick: () -> Unit,
    onShareCrashLogClick: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    SettingsGroup(title = stringResource(R.string.about_title)) {
        // Developer credit
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = KraftSpacing.Spacing12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            coil3.compose.AsyncImage(
                model = stringResource(R.string.github_avatar_url),
                contentDescription = stringResource(R.string.about_developer),
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
            Spacer(Modifier.width(KraftSpacing.Spacing12))
            Column {
                Text(
                    text = stringResource(R.string.about_developer),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.about_developer_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Text(
            text = stringResource(R.string.version_format, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = KraftSpacing.Spacing12),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        AboutRow(
            title = stringResource(R.string.github_title),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                context.startActivity(intent)
            },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        AboutRow(
            title = stringResource(R.string.privacy_title),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onPrivacyClick()
            },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        val licensesUrl = stringResource(R.string.licenses_url)
        AboutRow(
            title = stringResource(R.string.licenses_title),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(licensesUrl))
                context.startActivity(intent)
            },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        AboutRow(
            title = stringResource(R.string.share_crash_log),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onShareCrashLogClick()
            },
        )
    }
}
