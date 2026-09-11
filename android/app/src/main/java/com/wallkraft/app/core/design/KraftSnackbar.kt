package com.wallkraft.app.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Apple-like dark pill snackbar — floats *above* the bottom glass bar, not
 * beneath it. Same 4-layer frost as the tab bar, pill shape, 8dp elevation.
 * Used for collection deleted / name exists / rotation failed etc.
 */
@Composable
fun KraftSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { data -> KraftSnackbar(data) },
    )
}

@Composable
private fun KraftSnackbar(data: SnackbarData) {
    // Opaque pill, just above the bar — not attached, small gap. Same pill shape as tab bar.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = KraftSpacing.Spacing24)
            .padding(bottom = KraftSpacing.GlassBarReserve + KraftSpacing.Spacing8),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(KraftRadius.Pill))
                .background(Color(0xFF1C1C1E), RoundedCornerShape(KraftRadius.Pill))
                .padding(horizontal = KraftSpacing.Spacing16, vertical = KraftSpacing.Spacing12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (data.visuals.actionLabel != null) {
                TextButton(
                    onClick = { data.performAction() },
                    modifier = Modifier.padding(start = KraftSpacing.Spacing12),
                ) {
                    Text(
                        text = data.visuals.actionLabel ?: "",
                        color = KraftColors.AuroraBlue,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
