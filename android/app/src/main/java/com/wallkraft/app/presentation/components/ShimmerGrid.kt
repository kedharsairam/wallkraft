package com.wallkraft.app.presentation.components

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing

// Precomputed heights — deterministic pattern, no Random allocation per recomposition.
private val placeholderHeights = listOf(220, 320, 260, 380, 240, 300, 350, 280, 290, 340, 250, 310)

/**
 * Shimmer loading grid — left-to-right sweep animation.
 *
 * Each tile uses a horizontal gradient that shifts from left to right,
 * matching the skeleton loading pattern used in modern apps.
 * Item count is viewport-adaptive: enough tiles to fill the screen.
 */
@Composable
fun ShimmerGrid(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    // Sweep offset: animates from -1f (off-screen left) to 2f (off-screen right)
    // EaseInOut for natural shimmer feel (Apple HIG: motion should feel organic).
    val sweepOffset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = EaseInOut),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerSweep",
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = KraftConstants.ShimmerGradientAlpha),
        MaterialTheme.colorScheme.surfaceVariant,
    )

    // Calculate viewport-appropriate item count based on actual screen height.
    val configuration = LocalConfiguration.current
    val viewportHeightDp = configuration.screenHeightDp.dp
    val itemCount = maxOf(6, (viewportHeightDp / 280.dp).toInt()) // ~280dp avg tile height

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(KraftSpacing.GridTileMin),
        contentPadding = PaddingValues(KraftSpacing.Spacing8),
        horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
        verticalItemSpacing = KraftSpacing.Spacing8,
        modifier = modifier.fillMaxSize(),
    ) {
        items(itemCount) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(placeholderHeights[index % placeholderHeights.size].dp)
                    .clip(RoundedCornerShape(KraftRadius.Standard))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .background(
                        Brush.linearGradient(
                            colors = shimmerColors,
                            start = Offset(sweepOffset * 400f, 0f),
                            end = Offset(sweepOffset * 400f + 400f, 0f),
                        ),
                    ),
            )
        }
    }
}
