package com.wallkraft.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.wallkraft.app.R
import com.wallkraft.app.core.cache.GridImageLoader
import com.wallkraft.app.core.design.KraftColors
import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.core.design.KraftIconSize
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.domain.model.Purity
import com.wallkraft.app.domain.model.Wallpaper

/**
 * A single wallpaper tile in the staggered grid.
 *
 * [sharedElementModifier] is built by the parent ([WallpaperGrid]) inside
 * [androidx.compose.animation.SharedTransitionScope] and applied to the image
 * so it can participate in the container-transform shared element transition.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WallpaperCard(
    wallpaper: Wallpaper,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    downloadedIds: Set<String> = emptySet(),
    onLongClick: (() -> Unit)? = null,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    sharedElementModifier: Modifier = Modifier,
) {
    // Use the wallpaper's true aspect ratio so the full image always fits the
    // tile — no cropping, no zooming. The generous clamp only guards against
    // bad metadata (zero/negative dimensions) and covers every real Wallhaven
    // ratio (down to 9:48 and up to 48:9).
    val w = wallpaper.dimensionX.toFloat().takeIf { it > 0f } ?: 1f
    val h = wallpaper.dimensionY.toFloat().takeIf { it > 0f } ?: 1f
    val ratio = (w / h).coerceIn(0.15f, 6f)

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val gridImageLoader = GridImageLoader.get()
        ?: ImageLoader.Builder(context.applicationContext).build()

    // Shared element stays on AsyncImage (not the outer Box) — putting it on
    // a clipped Box breaks the return transition: Compose resolves the pop
    // target to (0,0) instead of the actual tile position. The clip is also
    // applied to the AsyncImage (after sharedElementModifier) so the image
    // content itself is clipped to rounded corners throughout the shared
    // element transition — without it, the overlay draws sharp corners until
    // the image lands back in the grid.
    // Purity border — orange for Sketchy, red for NSFW (matches Wallhaven).
    // Subtle: 1dp with reduced alpha so it hints without dominating.
    val purityBorder = when (wallpaper.purityEnum) {
        Purity.Sketchy -> BorderStroke(2.dp, KraftColors.AuroraOrange)
        Purity.NSFW -> BorderStroke(2.dp, KraftColors.AuroraRed)
        else -> null
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(KraftRadius.Standard))
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClick()
                        },
                    )
                } else {
                    Modifier.combinedClickable(onClick = onClick)
                },
            ),
    ) {
        AsyncImage(
            // Ratio-preserving thumbnail (`thumbs.original`, ~300px): tiny
            // payload for a smooth grid and shows the full image in its true
            // ratio. Never the full-resolution `path` — that stays on the
            // detail screen. Null (no thumbs at all) shows the placeholder.
            model = wallpaper.thumbnail,
            contentDescription = wallpaper.resolution,
            // ContentScale.Fit matches the detail screen so the shared element
            // transition renders identical content on both sides. The tile's
            // aspect ratio already matches the image's true ratio, so Fit
            // looks identical to Crop — no visible change to the grid.
            contentScale = ContentScale.Fit,
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
                // sharedElement MUST come first — coordinate-changing modifiers
                // (graphicsLayer, offset, alpha) go after it per Compose docs.
                // On the image (NOT the outer Box) so pop return resolves
                // the correct tile position in the LazyGrid.
                // Clip AFTER sharedElementModifier: ensures the image content is
                // clipped to rounded corners throughout the shared element
                // transition — without it, the overlay draws sharp corners until
                // the image lands back in the grid. The outer Box also clips for
                // combinedClickable, but this inner clip is needed during the
                // transition when the image is drawn outside its parent bounds.
                .then(sharedElementModifier)
                .clip(RoundedCornerShape(KraftRadius.Standard))
                .then(purityBorder?.let { Modifier.border(it, RoundedCornerShape(KraftRadius.Standard)) } ?: Modifier)
                .fillMaxWidth()
                .aspectRatio(ratio)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )

        // Downloaded indicator badge.
        if (wallpaper.id in downloadedIds && !selectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(KraftSpacing.Spacing4)
                    .size(KraftIconSize.Medium)
                    .clip(CircleShape)
                    .background(KraftColors.AccentGreen.copy(alpha = KraftConstants.BadgeAlpha)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = stringResource(R.string.favorites_downloaded),
                    tint = Color.White,
                    modifier = Modifier.size(KraftIconSize.Tiny),
                )
            }
        }

        // Selection check overlay.
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(KraftSpacing.Spacing4)
                    .size(KraftIconSize.Large)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else Color.Black.copy(alpha = KraftConstants.SelectionOverlayAlpha),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = if (selected) stringResource(R.string.selected) else stringResource(R.string.not_selected),
                    tint = Color.White,
                    modifier = Modifier.size(KraftIconSize.Medium),
                )
            }
        }
    }
}
