package com.wallkraft.app.core.design

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    Column(modifier = modifier) {
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
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = KraftConstants.DividerAlpha),
        )
    }
}
