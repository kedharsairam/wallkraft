package com.wallkraft.app.util

import com.wallkraft.app.domain.model.CropRect
import com.wallkraft.app.domain.model.RotationMode

/**
 * Pure rotation framing math — no Android dependencies, JVM unit-tested.
 *
 * All rects are normalized source-image coordinates (0..1).
 */
object RotationFraming {

    private const val FULL = 1f

    /** Aspect tolerance for reusing a saved crop (relative error). */
    const val ASPECT_TOLERANCE = 0.05f

    /**
     * Selects the source crop for [mode]:
     * - SHOWCASE / ATMOSPHERE: full image (blur handles the fit).
     * - FILL: the user's saved crop when valid and matching the screen
     *   aspect, otherwise a center-crop to the screen aspect.
     */
    fun frameRect(
        srcW: Int,
        srcH: Int,
        dstW: Int,
        dstH: Int,
        saved: CropRect?,
        mode: RotationMode,
    ): CropRect {
        if (srcW <= 0 || srcH <= 0 || dstW <= 0 || dstH <= 0) {
            return CropRect(0f, 0f, FULL, FULL)
        }
        if (mode != RotationMode.FILL) return CropRect(0f, 0f, FULL, FULL)
        val dstAspect = dstW.toFloat() / dstH
        val srcAspect = srcW.toFloat() / srcH
        if (saved != null && saved.isValid()) {
            val err = kotlin.math.abs(saved.aspect(srcAspect) - dstAspect) / dstAspect
            if (err <= ASPECT_TOLERANCE) return saved
        }
        return centerCrop(srcAspect, dstAspect)
    }

    /** Largest centered crop of [srcAspect] matching [dstAspect]. */
    fun centerCrop(srcAspect: Float, dstAspect: Float): CropRect {
        if (srcAspect <= 0f || dstAspect <= 0f) return CropRect(0f, 0f, FULL, FULL)
        return if (srcAspect > dstAspect) {
            // Wider than target: trim the sides.
            val w = dstAspect / srcAspect
            val left = (FULL - w) / 2f
            CropRect(left, 0f, left + w, FULL)
        } else {
            // Taller than target: trim top and bottom.
            val h = srcAspect / dstAspect
            val top = (FULL - h) / 2f
            CropRect(0f, top, FULL, top + h)
        }
    }
}
