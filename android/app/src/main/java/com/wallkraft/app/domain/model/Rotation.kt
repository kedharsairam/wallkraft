package com.wallkraft.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wallpaper rotation schedule. */
enum class RotationSchedule {
    OFF,
    DAILY,
    WEEKLY,
}

/** How a rotated wallpaper is framed on a portrait screen. */
enum class RotationMode {
    /** Sharp fit image on a blurred cover background — nothing cropped. */
    SHOWCASE,
    /** Full-screen blur + dim (Muzei-style atmosphere). */
    ATMOSPHERE,
    /** Plain center-crop to fill (or the user's saved crop when present). */
    FILL,
}

/** Which screen(s) rotation applies to. */
enum class RotationTarget {
    HOME,
    LOCK,
    BOTH,
}

/**
 * A crop rectangle in normalized source-image coordinates (0..1).
 *
 * Saved when the user frames a wallpaper in the crop dialog; rotation
 * re-applies the user's own framing instead of guessing.
 */
@Serializable
data class CropRect(
    @SerialName("l") val left: Float = 0f,
    @SerialName("t") val top: Float = 0f,
    @SerialName("r") val right: Float = 1f,
    @SerialName("b") val bottom: Float = 1f,
) {
    /** True when the rect is sane and covers a meaningful area. */
    fun isValid(): Boolean =
        left >= 0f && top >= 0f && right <= 1f && bottom <= 1f &&
            right - left > 0.05f && bottom - top > 0.05f

    /** Clamps every edge into 0..1. */
    fun coerced(): CropRect =
        CropRect(
            left.coerceIn(0f, 1f),
            top.coerceIn(0f, 1f),
            right.coerceIn(0f, 1f),
            bottom.coerceIn(0f, 1f),
        )

    /**
     * True aspect ratio (width / height) of the cropped region, given the
     * source image aspect. Normalized widths must be scaled by it — a 0.19
     * wide slice of a 2.37-aspect panorama is a 0.45 (phone) frame.
     */
    fun aspect(srcAspect: Float): Float =
        ((right - left) * srcAspect / (bottom - top)).coerceAtLeast(1e-6f)
}
