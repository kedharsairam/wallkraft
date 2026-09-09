package com.wallkraft.app.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Unified top bar matching Browse's SearchFilterBar height.
 *
 * Browse: statusBarsPadding + 8dp top + 44dp field + 8dp bottom + divider.
 * This bar uses the same metrics so Favorites/Downloads/Settings feel identical.
 */
@Composable
fun KraftTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    // Exact match to bottom pill — Apple 4-layer stack (identical light/dark, ~82% opaque)
    // Bottom uses these 4 backgrounds on the Row inside GlassBox; top uses same here.
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.22f))
            .background(Color.White.copy(alpha = 0.22f))
            .background(Color(0xFF3A3A3C).copy(alpha = 0.45f))
            .background(Color(0xFF2C2C2E).copy(alpha = 0.15f)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = KraftSpacing.Spacing16, vertical = KraftSpacing.Spacing8)
                .height(KraftSpacing.TopBarHeight),
        ) {
            if (navigationIcon != null) {
                navigationIcon()
                Spacer(Modifier.width(KraftSpacing.Spacing8))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (actions != null) {
                Row(verticalAlignment = Alignment.CenterVertically) { actions() }
            }
        }
        // No divider — bottom pill has no divider, only elevation shadow via GlassBox.
        // Top uses same frost stack, no hairline, so both read as one material.
    }
}
