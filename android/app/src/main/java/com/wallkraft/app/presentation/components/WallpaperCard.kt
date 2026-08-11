package com.wallkraft.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftColors
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.domain.model.Wallpaper

/**
 * A single wallpaper tile in the staggered grid.
 */
@Composable
fun WallpaperCard(
    wallpaper: Wallpaper,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    downloadedIds: Set<String> = emptySet(),
) {
    // Use the wallpaper's true aspect ratio so the full image always fits the
    // tile — no cropping, no zooming. The generous clamp only guards against
    // bad metadata (zero/negative dimensions) and covers every real Wallhaven
    // ratio (down to 9:48 and up to 48:9).
    val w = wallpaper.dimensionX.toFloat().takeIf { it > 0f } ?: 1f
    val h = wallpaper.dimensionY.toFloat().takeIf { it > 0f } ?: 1f
    val ratio = (w / h).coerceIn(0.15f, 6f)

    val context = LocalContext.current
    val gridImageLoader = GridImageLoader.get()
        ?: ImageLoader.Builder(context.applicationContext).build()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(KraftRadius.Standard))
            .clickable { onClick() },
    ) {
        AsyncImage(
            // Ratio-preserving thumbnail (`thumbs.original`, ~300px): tiny
            // payload for a smooth grid and shows the full image in its true
            // ratio. Never the full-resolution `path` — that stays on the
            // detail screen. Null (no thumbs at all) shows the placeholder.
            model = wallpaper.thumbnail,
            contentDescription = wallpaper.resolution,
            contentScale = ContentScale.Crop,
            // No crossfade in the grid: fading every tile as it scrolls into
            // view adds per-frame compositing work and makes scrolling feel
            // janky. Tiles pop in instantly instead — smoothness comes from
            // prefetching (images are ready before they scroll in), not from
            // per-tile animations. (The singleton loader crossfades by default,
            // so pass a dedicated no-crossfade loader.)
            imageLoader = gridImageLoader,
            placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
            error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )

        // Downloaded indicator badge.
        if (wallpaper.id in downloadedIds) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(KraftSpacing.Spacing4)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(KraftColors.AccentGreen.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = stringResource(R.string.favorites_downloaded),
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}
