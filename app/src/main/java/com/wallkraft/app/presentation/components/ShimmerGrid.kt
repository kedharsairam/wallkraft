package com.wallkraft.app.presentation.components

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private val placeholderHeights = listOf(220, 320, 260, 380, 240, 300, 350, 280)

/** Pulsing placeholder tiles shown while the first page loads. */
@Composable
fun ShimmerGrid(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerAlpha",
    )

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(KraftSpacing.Spacing8),
        horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
        verticalItemSpacing = KraftSpacing.Spacing8,
        modifier = modifier.fillMaxSize(),
    ) {
        items(12) { index ->
            val seed = index * 7 + 3
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(placeholderHeights[Random(seed).nextInt(placeholderHeights.size)].dp)
                    .clip(RoundedCornerShape(KraftRadius.Standard))
                    .alpha(alpha)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}
