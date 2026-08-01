package com.wallkraft.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.wallkraft.app.core.design.KraftColors
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.domain.model.Wallpaper

/** A single wallpaper tile in the staggered grid. */
@Composable
fun WallpaperCard(
    wallpaper: Wallpaper,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Use the wallpaper's true aspect ratio so the full image always fits the
    // tile — no cropping, no zooming. The generous clamp only guards against
    // bad metadata (zero/negative dimensions) and covers every real Wallhaven
    // ratio (down to 9:48 and up to 48:9).
    val w = wallpaper.dimensionX.toFloat().takeIf { it > 0f } ?: 1f
    val h = wallpaper.dimensionY.toFloat().takeIf { it > 0f } ?: 1f
    val ratio = (w / h).coerceIn(0.15f, 6f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(KraftRadius.Standard))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            // Ratio-preserving thumbnail (`thumbs.original`, ~300px): tiny
            // payload for a smooth grid and shows the full image in its true
            // ratio. Never the full-resolution `path` — that stays on the
            // detail screen. Null (no thumbs at all) shows the placeholder.
            model = wallpaper.thumbnail,
            contentDescription = wallpaper.resolution,
            contentScale = ContentScale.Crop,
            placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
            error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )

        // Bottom scrim with favorites count.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                    ),
                )
                .padding(horizontal = KraftSpacing.Spacing8, vertical = KraftSpacing.Spacing4),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = wallpaper.favorites.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(start = KraftSpacing.Spacing4),
                )
            }
        }

        // Category indicator dot.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(KraftSpacing.Spacing4)
                .size(8.dp)
                .clip(CircleShape)
                .background(wallpaper.categoryColor()),
        )
    }
}

private fun Wallpaper.categoryColor(): Color = when (category) {
    "anime" -> KraftColors.AccentPurple
    "people" -> KraftColors.AccentOrange
    else -> KraftColors.AccentGreen
}
