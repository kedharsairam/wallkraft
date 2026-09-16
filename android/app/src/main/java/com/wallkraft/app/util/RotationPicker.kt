/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Kedhar Sairam
 */
package com.wallkraft.app.util

import com.wallkraft.app.domain.model.RotationSchedule
import com.wallkraft.app.domain.model.RotationTarget
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.model.WallpaperPosition

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

    /**
     * Restricts favorites to a source collection. Null [memberIds] means "all
     * sources" (no collection selected); a non-null set filters by membership,
     * preserving favorites order. A missing collection resolves to an empty
     * set upstream, yielding an empty pool.
     */
    fun filterByCollection(
        favorites: List<Wallpaper>,
        memberIds: Set<String>?,
    ): List<Wallpaper> {
        if (memberIds == null) return favorites
        return favorites.filter { it.id in memberIds }
    }

    /** Keeps only candidates whose local file exists. [hasFile] is predicate-injected so JVM tests pass a fake map. */
    fun filterAvailable(
        candidates: List<Wallpaper>,
        hasFile: (String) -> Boolean,
    ): List<Wallpaper> = candidates.filter { hasFile(it.id) }

    /** Maps a rotation target to the wallpaper position it applies to. */
    fun mapTarget(target: RotationTarget): WallpaperPosition =
        when (target) {
            RotationTarget.HOME -> WallpaperPosition.HOME
            RotationTarget.LOCK -> WallpaperPosition.LOCK
            RotationTarget.BOTH -> WallpaperPosition.BOTH
        }

    /**
     * Indices to attempt, starting at [startIndex] and wrapping around
     * [count] candidates, up to [maxAttempts] tries. Retries the same
     * candidate when fewer candidates exist than attempts (count=1 → [0,0,0]);
     * empty when [count] is 0.
     */
    fun retryOrder(
        startIndex: Int,
        count: Int,
        maxAttempts: Int = 3,
    ): List<Int> {
        if (count <= 0) return emptyList()
        return List(maxAttempts) {
            (((startIndex + it) % count) + count) % count
        }
    }

    /**
     * Whether the worker should proceed. A manual run (not a chain link) is
     * an explicit Rotate-now tap and must run even when the schedule is OFF;
     * a chain link must terminate quietly when OFF (the schedule may have
     * been turned off while the link was pending).
     */
    fun shouldContinueChain(
        isChain: Boolean,
        schedule: RotationSchedule,
    ): Boolean {
        if (!isChain) return true
        return schedule != RotationSchedule.OFF
    }
}
