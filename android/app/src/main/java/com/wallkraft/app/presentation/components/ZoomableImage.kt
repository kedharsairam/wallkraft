package com.wallkraft.app.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.wallkraft.app.core.design.KraftConstants
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ZoomableImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholderModel: Any? = null,
    onTap: () -> Unit = {},
    onZoomChanged: (Float) -> Unit = {},
    onLoaded: () -> Unit = {},
    loadFullRes: Boolean = true,
    zoomLevels: List<Float> = listOf(2.5f, KraftConstants.MaxCropZoom),
    imageWidth: Int = 0,
    imageHeight: Int = 0,
    sharedElementModifier: Modifier = Modifier,
    resetZoomSignal: Int = 0,
    clipRadius: androidx.compose.ui.unit.Dp = 0.dp,
) {
    var elementSize by remember { mutableStateOf(IntSize.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    // Fit scale: how much ContentScale.Fit scales the image to fit inside the
    // viewport. At scale=1 the full image is visible (black bars on one axis).
    val fitScale = if (imageWidth > 0 && imageHeight > 0 &&
        elementSize.width > 0 && elementSize.height > 0
    ) {
        minOf(
            elementSize.width.toFloat() / imageWidth,
            elementSize.height.toFloat() / imageHeight,
        )
    } else 1f

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()
    var animJob by remember { mutableStateOf<Job?>(null) }
    // Gate gestures until layout is ready and the shared-element fly-in (220ms)
    // has settled — prevents the rare overshoot when you scroll fast, open, and
    // double-tap on the very first frame while viewport/elementSize are still 0
    // or the container-transform is still driving bounds.
    var gesturesReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(280)
        gesturesReady = true
    }

    LaunchedEffect(scale) { onZoomChanged(scale) }

    fun clamp(x: Float, y: Float, s: Float): Offset {
        if (imageWidth <= 0 || imageHeight <= 0 || elementSize.width <= 0) {
            val scaledW = elementSize.width * s
            val scaledH = elementSize.height * s
            val maxX = maxOf(0f, (scaledW - viewportSize.width) / 2f)
            val maxY = maxOf(0f, (scaledH - viewportSize.height) / 2f)
            // Centered when not covering; bounded pan when covering.
            if (scaledW <= viewportSize.width && scaledH <= viewportSize.height) {
                val cx = (viewportSize.width - scaledW) / 2f
                val cy = (viewportSize.height - scaledH) / 2f
                return Offset(cx, cy)
            }
            return Offset(x.coerceIn(-maxX, maxX), y.coerceIn(-maxY, maxY))
        }
        val fitW = imageWidth * fitScale
        val fitH = imageHeight * fitScale
        val displayedW = fitW * s
        val displayedH = fitH * s
        val scaledInnerW = elementSize.width * s
        val scaledInnerH = elementSize.height * s
        val fillRelative = if (fitW > 0 && fitH > 0) {
            maxOf(viewportSize.width / fitW, viewportSize.height / fitH)
        } else 1f
        // Below fill: image doesn't cover viewport, lock to centered position
        // so no black-bar drift. At fill and above, allow covering pan.
        if (s < fillRelative - 0.01f) {
            val cx = (viewportSize.width - scaledInnerW) / 2f
            val cy = (viewportSize.height - scaledInnerH) / 2f
            return Offset(cx, cy)
        }
        // Covering: keep displayed image over the viewport (no bars)
        val lowerX = viewportSize.width - (scaledInnerW + displayedW) / 2f
        val upperX = -(scaledInnerW - displayedW) / 2f
        val lowerY = viewportSize.height - (scaledInnerH + displayedH) / 2f
        val upperY = -(scaledInnerH - displayedH) / 2f
        // When one axis is exactly fill, lower==upper (locked); when both
        // overflow, range allows free pan.
        return Offset(
            x.coerceIn(minOf(lowerX, upperX), maxOf(lowerX, upperX)),
            y.coerceIn(minOf(lowerY, upperY), maxOf(lowerY, upperY)),
        )
    }

    fun animateTo(endScale: Float, endOffset: Offset) {
        animJob?.cancel()
        val startScale = scale
        val startX = offset.x
        val startY = offset.y
        val anim = Animatable(0f)
        animJob = scope.launch {
            anim.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)) {
                val t = value
                scale = startScale + (endScale - startScale) * t
                offset = Offset(
                    startX + (endOffset.x - startX) * t,
                    startY + (endOffset.y - startY) * t,
                )
            }
        }
    }

    // External reset (e.g. back while zoomed) — animate scale/offset → fit
    // so the shared-element exit can carry a single smooth motion instead of
    // snap-then-shrink. Same 220ms spec as bounds animation.
    LaunchedEffect(resetZoomSignal) {
        if (resetZoomSignal != 0 && (scale > 1.01f || offset != Offset.Zero)) {
            animateTo(1f, Offset.Zero)
        }
    }

    val context = LocalContext.current
    val fullRequest = remember(model) {
        ImageRequest.Builder(context)
            .data(model)
            .size(Size.ORIGINAL)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { viewportSize = it },
        contentAlignment = Alignment.Center,
    ) {
        // sharedElement MUST come before graphicsLayer — Compose docs require
        // coordinate-changing modifiers (graphicsLayer, offset, alpha) to be
        // placed AFTER sharedElement so the bounds animation isn't overridden.
        // Clip morphs 12dp→0dp in sync with backgroundAlpha so corners don't pop.
        Box(
            modifier = Modifier
                .then(sharedElementModifier)
                .then(if (clipRadius > 0.dp) Modifier.clip(RoundedCornerShape(clipRadius)) else Modifier)
                .fillMaxSize()
                .onSizeChanged { elementSize = it }
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        ) {
            if (placeholderModel != null) {
                AsyncImage(
                    model = placeholderModel,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (loadFullRes) {
                AsyncImage(
                    model = fullRequest,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    onSuccess = { onLoaded() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(gesturesReady, viewportSize, elementSize) {
                    // Pinch/pan — ignore until layout ready and fly-in done.
                    if (!gesturesReady || viewportSize == IntSize.Zero || elementSize == IntSize.Zero) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        // Re-check after down — a fast open may still be settling.
                        if (!gesturesReady || viewportSize == IntSize.Zero || elementSize == IntSize.Zero) return@awaitEachGesture
                        animJob?.cancel()
                        do {
                            val event = awaitPointerEvent()
                            val pointerCount = event.changes.count { it.pressed }
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid()
                            var newX = offset.x + pan.x
                            var newY = offset.y + pan.y
                            if (pointerCount >= 2 && kotlin.math.abs(zoom - 1f) > 0.02f) {
                                val newScale = (scale * zoom)
                                    .coerceIn(1f, KraftConstants.MaxCropZoom)
                                val k = newScale / scale
                                newX = centroid.x + (newX - centroid.x) * k
                                newY = centroid.y + (newY - centroid.y) * k
                                scale = newScale
                            }
                            if (scale > 1.01f || pointerCount >= 2) {
                                offset = clamp(newX, newY, scale)
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .pointerInput(gesturesReady, viewportSize, elementSize) {
                    detectTapGestures(
                        onDoubleTap = { tapped ->
                            // Ignore until layout is valid and fly-in has settled.
                            if (!gesturesReady || viewportSize == IntSize.Zero || elementSize == IntSize.Zero) return@detectTapGestures
                            // Cycle through zoom levels: fit → fill → native → fit.
                            // Find the current level and advance to the next.
                            val currentLevel = zoomLevels.indexOfFirst {
                                kotlin.math.abs(scale - it) < 0.05f
                            }
                            val nextIndex = if (currentLevel >= 0) {
                                (currentLevel + 1) % zoomLevels.size
                            } else {
                                // Not at any defined level (e.g. pinch-zoomed to
                                // an in-between scale) — start from the first.
                                0
                            }
                            val targetScale = zoomLevels[nextIndex]
                                .coerceIn(1f, KraftConstants.MaxCropZoom)
                            if (targetScale <= 1.01f) {
                                animateTo(1f, Offset.Zero)
                            } else {
                                // Center the image so no black bars on either axis.
                                val targetOffset = Offset(
                                    (viewportSize.width - viewportSize.width * targetScale) / 2f,
                                    (viewportSize.height - viewportSize.height * targetScale) / 2f,
                                )
                                animateTo(targetScale, targetOffset)
                            }
                        },
                        onTap = { onTap() },
                    )
                },
        )
    }
}
