package com.wallkraft.app.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftIconSize
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing

/**
 * One-shot welcome card for boundary rotation timing. Shown once (fresh
 * installs and upgraders alike) — the framework holds one card today and
 * accepts more later without redesign. One idea, three lines, one button:
 * information without slabs.
 */
@Composable
fun RotationTimingWelcome(onDone: () -> Unit) {
    Dialog(onDismissRequest = onDone) {
        Surface(
            shape = RoundedCornerShape(KraftRadius.Large),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(KraftSpacing.Spacing20),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(KraftIconSize.XLarge),
                )
                Spacer(Modifier.height(KraftSpacing.Spacing12))
                Text(
                    text = stringResource(R.string.welcome_rotation_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(KraftSpacing.Spacing8))
                Text(
                    text = stringResource(R.string.welcome_rotation_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(KraftSpacing.Spacing12))
                TextButton(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.done))
                }
            }
        }
    }
}
