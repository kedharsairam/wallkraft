package com.wallkraft.app.util

import com.wallkraft.app.domain.model.Wallpaper

/**
 * Pure rotation candidate logic — no Android dependencies, JVM unit-tested.
 */
object RotationPicker {

    /** Wallpapers smaller than this (on either side) are skipped — blurry output. */
    const val MIN_MIN_DIMENSION = 720

    /** Candidates sharp enough to rotate, preserving list order. */
    fun candidates(
        wallpapers: List<Wallpaper>,
        minMinDimension: Int = MIN_MIN_DIMENSION,
    ): List<Wallpaper> =
        wallpapers.filter {
            it.dimensionX >= minMinDimension && it.dimensionY >= minMinDimension
        }

    /**
     * Round-robin cursor into a candidate list. Returns -1 when empty.
     * Wraps stale/negative cursors so schedule edits can never wedge it.
     */
    fun pickNext(count: Int, lastIndex: Int): Int {
        if (count <= 0) return -1
        val next = (lastIndex + 1) % count
        return if (next < 0) next + count else next
    }
}
