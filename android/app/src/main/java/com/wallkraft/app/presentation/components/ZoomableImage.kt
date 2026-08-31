package com.wallkraft.app.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.ui.util.lerp

private const val MAX_SCALE = 8f
private const val ANIM_DURATION_MS = 220L

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
    zoomLevels: List<Float> = listOf(2.5f, MAX_SCALE),
    imageWidth: Int = 0,
    imageHeight: Int = 0,
    sharedElementModifier: Modifier = Modifier,
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
    var zoomIndex by remember(zoomLevels) { mutableIntStateOf(zoomLevels.lastIndex) }

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
        val startOffset = offset
        animJob = scope.launch {
            val startTime = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                val t = ((now - startTime) / 1_000_000L).toFloat() / ANIM_DURATION_MS.toFloat()
                val eased = FastOutSlowInEasing.transform(t.coerceIn(0f, 1f))
                scale = lerp(startScale, endScale, eased)
                offset = lerp(startOffset, endOffset, eased)
                if (t >= 1f) break
            }
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
        Box(
            modifier = Modifier
                .then(sharedElementModifier)
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
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
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
                                    .coerceIn(1f, MAX_SCALE)
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
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapped ->
                            zoomIndex = (zoomIndex + 1) % zoomLevels.size
                            val targetScale = zoomLevels[zoomIndex].coerceIn(1f, MAX_SCALE)
                            if (targetScale <= 1.01f) {
                                animateTo(1f, Offset.Zero)
                            } else if (zoomIndex == 0) {
                                // First level = fill (no black bars). The inner Box is
                                // viewport-sized and scaled around top-left; its child
                                // image is Fit-centered inside it. To center the final
                                // displayed image in the viewport, offset must be
                                // (viewport - viewport*scale)/2 on both axes.
                                val targetOffset = Offset(
                                    (viewportSize.width - viewportSize.width * targetScale) / 2f,
                                    (viewportSize.height - viewportSize.height * targetScale) / 2f,
                                )
                                animateTo(targetScale, targetOffset)
                            } else {
                                val k = targetScale / scale
                                val target = clamp(
                                    tapped.x + (offset.x - tapped.x) * k,
                                    tapped.y + (offset.y - tapped.y) * k,
                                    targetScale,
                                )
                                animateTo(targetScale, target)
                            }
                        },
                        onTap = { onTap() },
                    )
                },
        )
    }
}
