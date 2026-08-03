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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import coil3.compose.AsyncImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 8f
private const val DOUBLE_TAP_SCALE = 2.5f
private const val ANIM_DURATION_MS = 220L

/**
 * A full-featured zoomable image:
 *  - pinch-to-zoom (always works, even starting from 1x), zooming toward the
 *    pinch centroid
 *  - one- or two-finger pan while zoomed, clamped to the image bounds
 *  - double-tap to zoom in to the tapped point (animated) or back to fit
 *  - at 1x, single-finger drags pass through so the page scrolls normally
 */
@Composable
fun ZoomableImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
) {
    var scale by remember { mutableFloatStateOf(MIN_SCALE) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var elementSize by remember { mutableStateOf(IntSize.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    var animJob by remember { mutableStateOf<Job?>(null) }

    fun clamp(x: Float, y: Float, s: Float): Offset {
        // Bounds by the VIEWPORT, not the element. When the scaled image is
        // smaller than the viewport in one axis (the letterbox case), that
        // axis can't pan at all — otherwise you can push the image into the
        // black bars. When larger, the image pans edge-to-edge so it can be
        // aligned to fill the screen.
        val scaledW = elementSize.width * s
        val scaledH = elementSize.height * s
        val maxX = if (scaledW > viewportSize.width) scaledW - viewportSize.width else 0f
        val maxY = if (scaledH > viewportSize.height) scaledH - viewportSize.height else 0f
        return Offset(x.coerceIn(-maxX, 0f), y.coerceIn(-maxY, 0f))
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

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { viewportSize = it },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { elementSize = it }
                .graphicsLayer {
                    // Scale around the top-left corner so the zoom-to-point math
                    // (centroid / double-tap anchoring) is exact. The default
                    // center origin shifts the anchor and drifts to a corner.
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        // A new touch cancels any in-flight double-tap animation
                        // so it can't fight the drag.
                        animJob?.cancel()
                        do {
                            val event = awaitPointerEvent()
                            val pointerCount = event.changes.count { it.pressed }

                            if (pointerCount >= 2 || scale > 1f) {
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val centroid = event.calculateCentroid()

                                // Always pan: single-finger drag, two-finger drag,
                                // or fingers drifting during a pinch. Amplify by the
                                // current zoom so the image responds consistently at
                                // every level: at 8x the visible content is 1/8 of
                                // the image, so a 1:1 screen-pixel pan would need ~7
                                // full-screen drags to traverse it. Panning in image
                                // space (offset += pan * scale) keeps it responsive.
                                var newX = offset.x + pan.x * scale
                                var newY = offset.y + pan.y * scale

                                // Only zoom when it's a real pinch (>2% change per
                                // event). Without this threshold, tiny distance
                                // changes while dragging two fingers fight the pan
                                // and make the image feel stuck.
                                if (kotlin.math.abs(zoom - 1f) > 0.02f) {
                                    val newScale = (scale * zoom)
                                        .coerceIn(MIN_SCALE, MAX_SCALE)
                                    val k = newScale / scale
                                    newX = centroid.x + (newX - centroid.x) * k
                                    newY = centroid.y + (newY - centroid.y) * k
                                    scale = newScale
                                }
                                offset = clamp(newX, newY, scale)
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapped ->
                            if (scale > 1f) {
                                // Animate back to fit.
                                animateTo(MIN_SCALE, Offset.Zero)
                            } else {
                                // Zoom in, anchoring the tapped point.
                                val k = DOUBLE_TAP_SCALE / scale
                                val target = clamp(
                                    tapped.x + (offset.x - tapped.x) * k,
                                    tapped.y + (offset.y - tapped.y) * k,
                                    DOUBLE_TAP_SCALE,
                                )
                                animateTo(DOUBLE_TAP_SCALE, target)
                            }
                        },
                        onTap = { onTap() },
                    )
                },
        )
    }
}
