package com.wallkraft.app.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.wallkraft.app.core.design.KraftColors
import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.core.design.KraftIconSize
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing

/** Circular action button for the detail screen — 44dp, glass background for contrast on any wallpaper. Pass [text] for a wider pill button with label. Pass neither icon nor text for an empty button. */
@Composable
internal fun DetailCircleButton(
    onClick: () -> Unit,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    text: String? = null,
    iconTint: Color = Color.White,
    borderColor: Color = KraftColors.GlassBorder,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .then(
                if (text != null) {
                    Modifier.widthIn(min = 100.dp).height(KraftSpacing.TouchTarget)
                } else {
                    Modifier.size(KraftSpacing.TouchTarget)
                }
            )
            .clip(CircleShape)
            .background(KraftColors.Glass)
            .border(KraftSpacing.BorderWidth, borderColor, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = if (text != null) KraftSpacing.Spacing12 else 0.dp),
    ) {
        if (text != null) {
            androidx.compose.material3.Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        } else if (icon != null) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(KraftIconSize.Medium),
            )
        }
    }
}

/**
 * A clickable tag chip — tapping it opens the tag-filtered grid. Uses the
 * accent blue treatment (tinted fill + border) so tags read as interactive.
 * Increased alpha for readability on any wallpaper (contrast >= 4.5:1).
 * Pass `clickable = false` for the invisible measurement pass.
 */
@Composable
internal fun DetailTagChip(name: String, onClick: () -> Unit, clickable: Boolean = true) {
    val shape = RoundedCornerShape(KraftRadius.Small)
    Text(
        text = "#$name",
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(shape)
            .background(KraftColors.AccentBlue.copy(alpha = KraftConstants.TagChipFillAlpha))
            .border(
                border = androidx.compose.foundation.BorderStroke(1.dp, KraftColors.AccentBlue.copy(alpha = KraftConstants.TagChipBorderAlpha)),
                shape = shape,
            )
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = KraftSpacing.Spacing8, vertical = KraftSpacing.Spacing4),
    )
}

/** A compact stat item (resolution, size, category) in the bottom panel. */
@Composable
internal fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
        modifier = modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(KraftIconSize.Small),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
        )
    }
}
