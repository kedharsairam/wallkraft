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

            // Top gradient scrim for status bar readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
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
                    .padding(horizontal = 24.dp, vertical = 12.dp),
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
                    .padding(bottom = 48.dp),
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
                        .size(48.dp)
                        .alpha(0.7f)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .clickable { onDismiss() },
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
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
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )

        // Right icons: signal + wifi + battery
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(6.dp)
                .background(Color.White, CircleShape),
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(9.dp)
                .background(Color.White, CircleShape),
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(12.dp)
                .background(Color.White, CircleShape),
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .background(Color.White, CircleShape),
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
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun BatteryIcon(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(10.dp)
                .background(Color.White, CircleShape),
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(5.dp)
                .background(Color.White, CircleShape),
        )
    }
}

@Composable
private fun LargeClock(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = "12",
            color = Color.White,
            fontSize = 86.sp,
            fontWeight = FontWeight.Thin,
            letterSpacing = (-2).sp,
        )
        Text(
            text = "00",
            color = Color.White,
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
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .size(28.dp)
                .offset { IntOffset(0, chevronOffset.value.roundToInt()) },
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.lockscreen_swipe),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
        )
    }
}
