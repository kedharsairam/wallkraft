package com.wallkraft.app.presentation.lockscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftColors
import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.core.design.KraftIconSize
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.core.design.KraftTypeScale
import com.wallkraft.app.domain.model.Wallpaper
import kotlin.math.roundToInt

/**
 * Full-screen lock screen preview overlay.
 *
 * Displays the wallpaper as it would appear on the lock screen, with a mock
 * status bar, large clock, and "swipe up to unlock" indicator. Tap or swipe
 * down to dismiss.
 */
@Composable
fun LockscreenPreview(
    wallpaper: Wallpaper,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageModel = wallpaper.path
    val dismissOffset = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
            initialOffsetY = { it },
        ) + fadeIn(animationSpec = tween(200)),
        exit = slideOutVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
            targetOffsetY = { it },
        ) + fadeOut(animationSpec = tween(150)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, dismissOffset.floatValue.roundToInt()) }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dismissOffset.floatValue > 100f) {
                                onDismiss()
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            if (dragAmount > 0f) {
                                // Only allow downward dragging
                                dismissOffset.floatValue =
                                    (dismissOffset.floatValue + dragAmount).coerceAtMost(0f)
                            }
                        },
                    )
                }
                .clickable { onDismiss() },
        ) {
            // Wallpaper background
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Top gradient scrim for status bar readability.
            // 200dp scrim height + 60dp clock offset are mock layout specs, not spacing scale.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                KraftColors.Background.copy(alpha = KraftConstants.OverlayCropScrimTop),
                                // Transparent end fades scrim into wallpaper — technically required.
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            // Status bar mock
            StatusBarMock(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = KraftSpacing.Spacing24, vertical = KraftSpacing.Spacing12),
            )

            // Center clock
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-60).dp),
            ) {
                LargeClock()
            }

            // Bottom section: swipe hint + camera shortcut
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = KraftSpacing.Spacing48),
            ) {
                // Swipe up to unlock
                SwipeUnlockHint(
                    modifier = Modifier.align(Alignment.BottomCenter),
                )

                // Camera shortcut (bottom-right)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(KraftSpacing.Spacing48)
                        .alpha(KraftConstants.ErrorIconAlpha)
                        .background(KraftColors.TextPrimary.copy(alpha = KraftConstants.OverlayCameraAlpha), CircleShape)
                        .clickable { onDismiss() },
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = KraftColors.TextPrimary,
                        modifier = Modifier.size(KraftIconSize.Compact),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBarMock(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Time
        Text(
            text = "12:00",
            color = KraftColors.TextPrimary,
            fontSize = KraftTypeScale.Subheadline,
            fontWeight = FontWeight.SemiBold,
        )

        // Right icons: signal + wifi + battery
        Row(
            horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing6),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Signal bars (simplified)
            SignalBars()
            // Wifi icon (simplified)
            WifiIcon()
            // Battery icon (simplified)
            BatteryIcon()
        }
    }
}

@Composable
private fun SignalBars(modifier: Modifier = Modifier) {
    // Mock iOS signal bars — fixed 3dp-wide bars at staggered heights, not spacing scale.
    Row(
        horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing2),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(KraftSpacing.Spacing6)
                .background(KraftColors.TextPrimary, CircleShape),
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(9.dp)
                .background(KraftColors.TextPrimary, CircleShape),
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(KraftSpacing.Spacing12)
                .background(KraftColors.TextPrimary, CircleShape),
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp) // tallest mock signal bar — fixed mock spec
                .background(KraftColors.TextPrimary, CircleShape),
        )
    }
}

@Composable
private fun WifiIcon(modifier: Modifier = Modifier) {
    // Simplified wifi arc representation
    Box(modifier = modifier) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowUp,
            contentDescription = null,
            tint = KraftColors.TextPrimary,
            modifier = Modifier.size(KraftIconSize.Small),
        )
    }
}

@Composable
private fun BatteryIcon(modifier: Modifier = Modifier) {
    // Mock battery — fixed 20x10 body + 2x5 nub, not spacing scale.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .width(KraftIconSize.Medium)
                .height(10.dp)
                .background(KraftColors.TextPrimary, CircleShape),
        )
        Box(
            modifier = Modifier
                .width(KraftSpacing.Spacing2)
                .height(5.dp)
                .background(KraftColors.TextPrimary, CircleShape),
        )
    }
}

@Composable
private fun LargeClock(modifier: Modifier = Modifier) {
    // Mock lockscreen clock — 86sp oversized display + tight -2sp tracking, not type scale.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = "12",
            color = KraftColors.TextPrimary,
            fontSize = 86.sp,
            fontWeight = FontWeight.Thin,
            letterSpacing = (-2).sp,
        )
        Text(
            text = "00",
            color = KraftColors.TextPrimary,
            fontSize = 86.sp,
            fontWeight = FontWeight.Thin,
            letterSpacing = (-2).sp,
        )
    }
}

@Composable
private fun SwipeUnlockHint(modifier: Modifier = Modifier) {
    val chevronOffset = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            chevronOffset.animateTo(
                targetValue = -8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
            chevronOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowUp,
            contentDescription = null,
            tint = KraftColors.TextPrimary.copy(alpha = KraftConstants.ErrorIconAlpha),
            modifier = Modifier
                .size(KraftSpacing.Spacing24 + KraftSpacing.Spacing4)
                .offset { IntOffset(0, chevronOffset.value.roundToInt()) },
        )
        Spacer(modifier = Modifier.height(KraftSpacing.Spacing4))
        Text(
            text = stringResource(R.string.lockscreen_swipe),
            color = KraftColors.TextPrimary.copy(alpha = KraftConstants.ErrorIconAlpha),
            fontSize = KraftTypeScale.Footnote,
            fontWeight = FontWeight.Normal,
        )
    }
}
