package com.wallkraft.app.presentation.detail

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.domain.model.Wallpaper
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * The bottom panel: drag handle, uploader row, stat pills, and tags.
 *
 * The panel always renders its full content and clips it to its current
 * height, so dragging reveals the extra content progressively with zero
 * content switching — the only thing that changes while dragging is the Box
 * height. All drag state lives here so per-frame drag updates recompose only
 * this composable, never the whole screen (which is what made the panel
 * stutter before).
 *
 * `expanded` is owned by the caller (the right-edge stack reads it to fade
 * out); this composable only reports it via [onExpandedChange] as the drag
 * crosses the threshold or settles past the expand threshold.
 */
@Composable
internal fun BottomPanel(
    wallpaper: Wallpaper,
    onTagClick: (String) -> Unit,
    onUploaderClick: (String) -> Unit,
    isUploaderDeleted: Boolean,
    collapsedHeightPx: Float,
    maxPanelHeightPx: Float,
    contentOverflows: Boolean,
    navBarPadding: androidx.compose.ui.unit.Dp,
    isZoomed: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    chromeAlpha: Float = 1f,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    // Settled height, animated between collapsed and expanded.
    val panelHeight = remember(wallpaper.id) { Animatable(0f) }
    // Drag offset in px, tracked synchronously so the release decision is
    // correct even though the Animatable settles asynchronously.
    var dragOffsetPx by remember(wallpaper.id) { mutableFloatStateOf(0f) }
    // Once the user has touched the panel we stop auto-snapping it to the
    // measured heights — the panel is theirs from then on.
    var userInteracted by remember(wallpaper.id) { mutableStateOf(false) }
    // Release velocity in px/s (upward positive), smoothed from the last drag
    // events so a quick flick settles the panel even when the pull distance
    // is small. Without this, expanding a tall panel (many tags) would need
    // a long drag because the settle threshold scales with content height.
    var dragVelocityPxPerSec by remember(wallpaper.id) { mutableFloatStateOf(0f) }
    var lastDragTimestamp by remember(wallpaper.id) { mutableLongStateOf(0L) }

    // Settle the panel to the target height. Runs on first layout and again
    // whenever the measured heights change (e.g. the real detail loads and
    // the uploader row appears). Once the user has dragged the panel it's
    // theirs — except when the content grows past the current height, so
    // the panel never clips its own content. `expanded` is deliberately NOT
    // a key here: onDragEnd owns the settle, and keying on it would let
    // this effect race the finger every time the drag threshold flips it.
    LaunchedEffect(collapsedHeightPx, maxPanelHeightPx, wallpaper.id) {
        if (collapsedHeightPx > 0f) {
            val target = if (expanded) maxPanelHeightPx else collapsedHeightPx
            if (!userInteracted || target > panelHeight.value) {
                panelHeight.animateTo(
                    targetValue = target,
                    animationSpec = SharedElementSpringFloat,
                )
            }
        }
    }

    // The panel is always anchored to the bottom bar: its height follows the
    // finger while dragging, clamped to the collapsed/expanded range, and it
    // grows upward from the bottom bar as it expands.
    val currentHeightPx = (panelHeight.value + dragOffsetPx)
        .coerceIn(collapsedHeightPx, maxPanelHeightPx)

    // Wrapper Box restores the BoxScope so the panel can align to the bottom
    // of the content area (BottomPanel is a standalone composable, not a
    // BoxScope receiver).
    Box(modifier = Modifier.fillMaxSize()) {
        // The outer panel is a custom Layout (not a Box) because Box
        // center-aligns children that are taller than the Box, even with
        // contentAlignment = TopStart. This pushes the handle/uploader/hint
        // above the visible clip region when collapsed. A custom Layout lets
        // us measure content at maxPanelHeight (so every child lays out at
        // its natural size) while always placing it at y=0. The clip +
        // background + pointerInput work on the Layout's reported height
        // (currentHeightPx), which is what drives the panel's visual size.
        val maxH = maxPanelHeightPx.toInt()
        Layout(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .zIndex(2f)
                .graphicsLayer { alpha = chromeAlpha * (if (isZoomed) 0f else 1f) }
                .height(with(density) { currentHeightPx.toDp() })
                .clip(RoundedCornerShape(topStart = KraftRadius.Large, topEnd = KraftRadius.Large))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .pointerInput(wallpaper.id, collapsedHeightPx, maxPanelHeightPx) {
                    // Settled height where the drag started. Read at release to
                    // tell an upward pull from a downward drag, so the expand and
                    // collapse thresholds can be asymmetric.
                    var dragStartPx = 0f
                    detectVerticalDragGestures(
                        onDragStart = {
                            // Freeze the settled height so the offset is relative
                            // to it; cancel any in-flight settle animation. From
                            // here on the panel is user-controlled.
                            userInteracted = true
                            dragVelocityPxPerSec = 0f
                            lastDragTimestamp = 0L
                            dragStartPx = panelHeight.value
                            scope.launch { panelHeight.stop() }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            // Track release velocity from the event timestamps.
                            // dragAmount is negative when dragging up; negate so
                            // upward is positive.
                            val now = change.uptimeMillis
                            if (lastDragTimestamp > 0L) {
                                val dt = (now - lastDragTimestamp).coerceAtLeast(1L)
                                val instantVelocity = -dragAmount / dt * 1000f
                                dragVelocityPxPerSec =
                                    dragVelocityPxPerSec * 0.7f + instantVelocity * 0.3f
                            }
                            lastDragTimestamp = now
                            // dragAmount is negative when dragging up; subtract so
                            // pulling up grows the panel. Clamp so the offset
                            // never exceeds the panel's range — prevents visual
                            // overshoot when dragging fast.
                            dragOffsetPx = (dragOffsetPx - dragAmount)
                                .coerceIn(collapsedHeightPx - panelHeight.value, maxPanelHeightPx - panelHeight.value)
                            // Flip state live while dragging: once the panel
                            // clears the collapsed bar by a small margin the
                            // right-edge stack fades out. The panel Box's height
                            // follows the finger, so its clip reveals the extra
                            // content progressively — no content switching.
                            onExpandedChange(
                                (panelHeight.value + dragOffsetPx) >
                                    collapsedHeightPx + with(density) { 8.dp.toPx() },
                            )
                        },
                        onDragEnd = {
                            val currentPx = (panelHeight.value + dragOffsetPx)
                                .coerceIn(collapsedHeightPx, maxPanelHeightPx)
                            val rangePx = maxPanelHeightPx - collapsedHeightPx
                            // Expand threshold: a small, consistent pull opens the
                            // panel regardless of content height. Capped at half
                            // the range so it always sits below the expanded
                            // height for very short content.
                            val expandThresholdPx = collapsedHeightPx +
                                min(with(density) { 40.dp.toPx() }, rangePx * 0.5f)
                            // Collapse needs a deliberate drag past the midpoint
                            // (or a downward flick) — a small downward drag on a
                            // tall panel shouldn't close it.
                            val midpointPx = (collapsedHeightPx + maxPanelHeightPx) / 2f
                            // A quick flick settles by velocity; a slow drag
                            // settles by position. Asymmetric thresholds: a small
                            // pull up opens the panel regardless of content
                            // height, while collapsing needs a deliberate drag
                            // down past the midpoint.
                            val velocityThresholdPx = with(density) { 380.dp.toPx() }
                            val draggedUp = currentPx > dragStartPx
                            val settleExpanded = when {
                                dragVelocityPxPerSec > velocityThresholdPx -> true
                                dragVelocityPxPerSec < -velocityThresholdPx -> false
                                draggedUp -> currentPx > expandThresholdPx
                                else -> currentPx > midpointPx
                            }
                            onExpandedChange(settleExpanded)
                            scope.launch {
                                // Snap to where the finger released, then
                                // animate to the target — no jump back.
                                // 220ms matches shared-element so panel settles
                                // in sync with chrome/background.
                                panelHeight.snapTo(currentPx)
                                panelHeight.animateTo(
                                    targetValue = if (settleExpanded) maxPanelHeightPx else collapsedHeightPx,
                                    animationSpec = SharedElementSpringFloat,
                                )
                            }
                            dragOffsetPx = 0f
                        },
                    )
                },
            content = {
                DetailPanelContent(
                    wallpaper = wallpaper,
                    onTagClick = onTagClick,
                    onUploaderClick = onUploaderClick,
                    isUploaderDeleted = isUploaderDeleted,
                    clickable = true,
                    collapsed = !expanded,
                    tagsScrollable = contentOverflows,
                    pullHintVisible = !expanded,
                    bottomPadding = navBarPadding,
                )
            },
            measurePolicy = { measurables: List<androidx.compose.ui.layout.Measurable>, constraints: Constraints ->
                val childConstraints = Constraints(
                    minWidth = constraints.minWidth,
                    maxWidth = constraints.maxWidth,
                    minHeight = maxH,
                    maxHeight = maxH,
                )
                val placeable = measurables.first().measure(childConstraints)
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(0, 0)
                }
            },
        )
    }
}
