/*
 * Liquid-glass engine adapted from Mortd3kay/liquid-glass-android
 * (https://github.com/Mortd3kay/liquid-glass-android), Copyright 2024 MRTDK,
 * Apache License 2.0 (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * Changes for WallKraft: per-frame println telemetry removed; used for a
 * single bottom tab capsule. AGSL path runs on API 33+; older releases fall
 * back to gradient simulation automatically.
 */
package com.wallkraft.app.presentation.components.glass

import android.annotation.SuppressLint
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.FloatRange
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlin.math.min
import kotlin.random.Random

internal data class GlassElement(
    val id: String,
    val position: Offset,
    val size: Size,
    val scale: Float,
    val blur: Float,
    val centerDistortion: Float,
    val cornerRadius: Float,
    val elevation: Float,
    val tint: Color,
    val darkness: Float,
    val warpEdges: Float,
) {
    // Check equality with tolerance for Float values
    fun equalsWithTolerance(other: GlassElement): Boolean {
        if (id != other.id) return false

        val tolerance = 0.01f
        val positionDiff = (position - other.position)
        val positionDistance =
            kotlin.math.sqrt(positionDiff.x * positionDiff.x + positionDiff.y * positionDiff.y)
        return positionDistance < tolerance &&
            kotlin.math.abs(size.width - other.size.width) < tolerance &&
            kotlin.math.abs(size.height - other.size.height) < tolerance &&
            kotlin.math.abs(scale - other.scale) < tolerance &&
            kotlin.math.abs(blur - other.blur) < tolerance &&
            kotlin.math.abs(centerDistortion - other.centerDistortion) < tolerance &&
            kotlin.math.abs(cornerRadius - other.cornerRadius) < tolerance &&
            kotlin.math.abs(elevation - other.elevation) < tolerance &&
            kotlin.math.abs(darkness - other.darkness) < tolerance &&
            kotlin.math.abs(warpEdges - other.warpEdges) < tolerance &&
            tint == other.tint
    }
}

interface GlassScope {
    fun Modifier.glassBackground(
        id: Long,
        scale: Float,
        blur: Float,
        centerDistortion: Float,
        shape: CornerBasedShape,
        elevation: Dp = 0.dp,
        tint: Color = Color.Transparent,
        darkness: Float = 0f,
        warpEdges: Float = 0f,
    ): Modifier
}

interface GlassBoxScope : BoxScope, GlassScope

@Composable
fun GlassBoxScope.GlassBox(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    @FloatRange(from = 0.0, to = 1.0)
    scale: Float = 0f,
    @FloatRange(from = 0.0, to = 1.0)
    blur: Float = 0f,
    @FloatRange(from = 0.0, to = 1.0)
    centerDistortion: Float = 0f,
    shape: CornerBasedShape = RoundedCornerShape(0.dp),
    elevation: Dp = 0.dp,
    tint: Color = Color.Transparent,
    @FloatRange(from = 0.0, to = 1.0)
    darkness: Float = 0f,
    @FloatRange(from = 0.0, to = 1.0)
    warpEdges: Float = 0f,
    content: @Composable BoxScope.() -> Unit = { },
) {
    val id = remember { Random.nextLong() }
    Box(
        modifier = modifier.glassBackground(
            id,
            scale.coerceIn(0f, 1f),
            blur.coerceIn(0f, 1f),
            centerDistortion.coerceIn(0f, 1f),
            shape,
            elevation,
            tint,
            darkness.coerceIn(0f, 1f),
            warpEdges.coerceIn(0f, 1f),
        ),
        contentAlignment, propagateMinConstraints, content,
    )
}

private class GlassBoxScopeImpl(
    boxScope: BoxScope,
    glassScope: GlassScope,
) : GlassBoxScope, BoxScope by boxScope,
    GlassScope by glassScope {

}

private class GlassScopeImpl(private val density: Density) : GlassScope {

    var updateCounter by mutableStateOf(0)
    val elements: MutableList<GlassElement> = mutableStateListOf()
    private val activeElements = mutableSetOf<String>()

    fun markElementAsActive(elementId: String) {
        activeElements.add(elementId)
    }

    fun cleanupInactiveElements() {
        val elementsToRemove = elements.filter { it.id !in activeElements }
        if (elementsToRemove.isNotEmpty()) {
            elements.removeAll { it.id !in activeElements }
            updateCounter++
        }
        activeElements.clear()
    }

    override fun Modifier.glassBackground(
        id: Long,
        scale: Float,
        blur: Float,
        centerDistortion: Float,
        shape: CornerBasedShape,
        elevation: Dp,
        tint: Color,
        darkness: Float,
        warpEdges: Float,
    ): Modifier = this
        .background(color = Color.Transparent, shape = shape)
        .onGloballyPositioned { coordinates ->
            val elementId = "glass_$id"
            markElementAsActive(elementId)

            val position = coordinates.positionInRoot()
            val size = coordinates.size.toSize()

            // Flat top bar (old shape) must not be pill-rounded like the bottom.
            // RoundedCornerShape(0.dp) signals a rect; Pill signals a capsule.
            val isFlat = shape == RoundedCornerShape(0.dp)
            val element = GlassElement(
                id = elementId,
                position = position,
                size = size,
                // True stadium radius: half the MIN dimension. Percent-based
                // CornerSize can resolve against the width (half of 1048px on
                // a 200px-tall pill), which turns the SDF capsule into a
                // pointed football. min() is correct for pills; flat rects
                // use 0 so the top bar stays edge-to-edge like the old shape.
                cornerRadius = if (isFlat) 0f else min(size.width, size.height) / 2f,
                scale = scale,
                blur = blur,
                centerDistortion = centerDistortion,
                elevation = with(density) { elevation.toPx() },
                tint = tint,
                darkness = darkness,
                warpEdges = warpEdges,
            )

            // Find existing element with same ID
            val existingIndex = elements.indexOfFirst { it.id == element.id }

            // Update only if element changed
            if (existingIndex == -1) {
                elements.add(element)
                updateCounter++
            } else {
                // Check if element changed with Float tolerance
                val existing = elements[existingIndex]
                if (!existing.equalsWithTolerance(element)) {
                    elements[existingIndex] = element
                    updateCounter++
                }
            }
        }
}

/**
 * Fallback implementation for Android versions < 13 (API 33)
 * Uses standard Compose modifiers to simulate glass effects
 */
private class GlassScopeFallbackImpl(private val density: Density) : GlassScope {

    override fun Modifier.glassBackground(
        id: Long,
        scale: Float,
        blur: Float,
        centerDistortion: Float,
        shape: CornerBasedShape,
        elevation: Dp,
        tint: Color,
        darkness: Float,
        warpEdges: Float,
    ): Modifier {
        // Create a glass-like effect using available modifiers
        val glassTint = if (tint == Color.Transparent) {
            Color.White.copy(alpha = 0.1f)
        } else {
            tint.copy(alpha = (tint.alpha * 0.9f).coerceIn(0f, 1f))
        }

        // Create a darker overlay for the darkness effect
        val darknessOverlay = if (darkness > 0f) {
            Color.Black.copy(alpha = darkness * 0.3f)
        } else {
            Color.Transparent
        }

        // Create a gradient for glass-like appearance
        val glassGradient = Brush.verticalGradient(
            colors = listOf(
                glassTint,
                glassTint.copy(alpha = glassTint.alpha * 0.7f),
                glassTint.copy(alpha = glassTint.alpha * 0.5f),
                glassTint,
            ),
        )

        return this
            // Apply glass gradient background
            .background(
                brush = glassGradient,
                shape = shape,
            )
            // Apply darkness overlay if needed
            .let { modifier ->
                if (darknessOverlay != Color.Transparent) {
                    modifier.background(
                        color = darknessOverlay,
                        shape = shape,
                    )
                } else {
                    modifier
                }
            }
            // Apply blur if available (API 31+)
            .let { modifier ->
                if (blur > 0f && android.os.Build.VERSION.SDK_INT >= 31) {
                    modifier.blur((blur * 20f).dp)
                } else {
                    modifier
                }
            }
            // Apply scale effect (limited simulation)
            .let { modifier ->
                if (scale > 0f) {
                    modifier.graphicsLayer {
                        scaleX = 1f + (scale * 0.1f)
                        scaleY = 1f + (scale * 0.1f)
                    }
                } else {
                    modifier
                }
            }
            // Apply transparency for warp edges effect
            .let { modifier ->
                if (warpEdges > 0f) {
                    modifier.alpha(1f - (warpEdges * 0.2f).coerceIn(0f, 0.8f))
                } else {
                    modifier
                }
            }
    }
}

@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    glassContent: @Composable GlassBoxScope.() -> Unit,
) {
    // Check if AGSL is supported (Android 13+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GlassContainerWithShader(modifier, content, glassContent)
    } else {
        GlassContainerFallback(modifier, content, glassContent)
    }
}

@Composable
fun GlassContainerWithHidden(
    modifier: Modifier = Modifier,
    hidden: Boolean,
    content: @Composable () -> Unit,
    glassContent: @Composable GlassBoxScope.() -> Unit,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GlassContainerWithShaderHidden(modifier, hidden, content, glassContent)
    } else {
        GlassContainerFallback(modifier, content, if (hidden) ({}) else glassContent)
    }
}

/**
 * Packs glass element data into shader uniforms.
 * Shared by [GlassContainerWithShader] and [GlassContainerWithShaderHidden].
 */
@SuppressLint("NewApi")
private fun applyGlassUniforms(
    shader: RuntimeShader,
    elements: List<GlassElement>,
    effectiveCount: Int,
) {
    shader.setIntUniform("elementsCount", effectiveCount)

    val maxElements = 10
    val positions = FloatArray(maxElements * 2)
    val sizes = FloatArray(maxElements * 2)
    val scales = FloatArray(maxElements)
    val radii = FloatArray(maxElements)
    val elevations = FloatArray(maxElements)
    val centerDistortions = FloatArray(maxElements)
    val tints = FloatArray(maxElements * 4)
    val darkness = FloatArray(maxElements)
    val warpEdges = FloatArray(maxElements)
    val blurs = FloatArray(maxElements)

    for (i in 0 until effectiveCount) {
        val element = elements[i]
        positions[i * 2] = element.position.x
        positions[i * 2 + 1] = element.position.y
        sizes[i * 2] = element.size.width
        sizes[i * 2 + 1] = element.size.height
        scales[i] = element.scale
        radii[i] = element.cornerRadius
        elevations[i] = element.elevation
        centerDistortions[i] = element.centerDistortion

        tints[i * 4] = element.tint.red
        tints[i * 4 + 1] = element.tint.green
        tints[i * 4 + 2] = element.tint.blue
        tints[i * 4 + 3] = element.tint.alpha

        darkness[i] = element.darkness
        warpEdges[i] = element.warpEdges
        blurs[i] = element.blur
    }

    shader.setFloatUniform("glassPositions", positions)
    shader.setFloatUniform("glassSizes", sizes)
    shader.setFloatUniform("glassScales", scales)
    shader.setFloatUniform("cornerRadii", radii)
    shader.setFloatUniform("elevations", elevations)
    shader.setFloatUniform("centerDistortions", centerDistortions)
    shader.setFloatUniform("glassTints", tints)
    shader.setFloatUniform("glassDarkness", darkness)
    shader.setFloatUniform("glassWarpEdges", warpEdges)
    shader.setFloatUniform("glassBlurs", blurs)
}

@SuppressLint("NewApi") // Version check is performed in GlassContainer
@Composable
private fun GlassContainerWithShader(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    glassContent: @Composable GlassBoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val glassScope = remember { GlassScopeImpl(density) }

    val shader = remember { RuntimeShader(getGlassShader(context)) }

    SideEffect {
        glassScope.cleanupInactiveElements()
    }

    DisposableEffect(Unit) {
        onDispose {
            glassScope.elements.clear()
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                shader.setFloatUniform("resolution", size.width, size.height)
                // Read updateCounter to trigger recomposition when elements change
                glassScope.updateCounter

                val elements = glassScope.elements
                val effectiveCount = minOf(elements.size, 10)

                applyGlassUniforms(shader, elements, effectiveCount)

                renderEffect = RenderEffect.createRuntimeShaderEffect(
                    shader, "contents",
                ).asComposeRenderEffect()
            },
    ) {
        content()
    }
    Box(modifier = modifier) {
        GlassBoxScopeImpl(this, glassScope).glassContent()
    }
}

@SuppressLint("NewApi")
@Composable
private fun GlassContainerWithShaderHidden(
    modifier: Modifier = Modifier,
    hidden: Boolean,
    content: @Composable () -> Unit,
    glassContent: @Composable GlassBoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val glassScope = remember { GlassScopeImpl(density) }

    val shader = remember { RuntimeShader(getGlassShader(context)) }

    SideEffect {
        if (hidden) {
            if (glassScope.elements.isNotEmpty()) {
                glassScope.elements.clear()
                glassScope.updateCounter++
            }
            glassScope.cleanupInactiveElements()
        } else {
            glassScope.cleanupInactiveElements()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            glassScope.elements.clear()
        }
    }

    // When hidden, immediately clear via LaunchedEffect as well
    androidx.compose.runtime.LaunchedEffect(hidden) {
        if (hidden) {
            glassScope.elements.clear()
            glassScope.updateCounter++
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                shader.setFloatUniform("resolution", size.width, size.height)
                // Read updateCounter to trigger recomposition when elements change
                glassScope.updateCounter

                // Force 0 elements when hidden, even if cleanup is delayed
                val effectiveCount = if (hidden) 0 else minOf(glassScope.elements.size, 10)
                val elements = if (hidden) emptyList() else glassScope.elements

                applyGlassUniforms(shader, elements, effectiveCount)

                if (effectiveCount == 0) {
                    renderEffect = null
                } else {
                    renderEffect = RenderEffect.createRuntimeShaderEffect(
                        shader, "contents",
                    ).asComposeRenderEffect()
                }
            },
    ) {
        content()
    }
    Box(modifier = modifier) {
        if (!hidden) {
            GlassBoxScopeImpl(this, glassScope).glassContent()
        }
    }
}

@Composable
private fun GlassContainerFallback(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    glassContent: @Composable GlassBoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val glassScope = remember { GlassScopeFallbackImpl(density) }

    Box(modifier = modifier) {
        content()
    }
    Box(modifier = modifier) {
        GlassBoxScopeImpl(this, glassScope).glassContent()
    }
}

@Volatile
private var cachedGlassShader: String? = null

private fun getGlassShader(context: android.content.Context): String {
    return cachedGlassShader ?: context.assets.open("shaders/glass_displacement.agsl")
        .bufferedReader().use { it.readText() }
        .also { cachedGlassShader = it }
}
