package com.wallkraft.app.util

import com.wallkraft.app.domain.model.RotationSchedule
import com.wallkraft.app.domain.model.RotationTarget
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.model.WallpaperPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RotationPickerTest {

    private fun wallpaper(id: String, w: Int = 1080, h: Int = 2400) =
        Wallpaper(id = id, dimensionX = w, dimensionY = h)

    @Test
    fun pickNext_empty_is_minus_one() {
        assertEquals(-1, RotationPicker.pickNext(0, -1))
    }

    @Test
    fun pickNext_round_robins_and_wraps() {
        assertEquals(0, RotationPicker.pickNext(3, -1))
        assertEquals(1, RotationPicker.pickNext(3, 0))
        assertEquals(2, RotationPicker.pickNext(3, 1))
        assertEquals(0, RotationPicker.pickNext(3, 2))
    }

    @Test
    fun pickNext_stale_cursor_wraps() {
        assertEquals(2, RotationPicker.pickNext(3, 10))
    }

    @Test
    fun candidates_skips_small_images() {
        val list = listOf(
            wallpaper("sharp"),
            wallpaper("tiny", w = 400, h = 300),
            wallpaper("narrow", w = 1080, h = 500),
        )
        assertEquals(listOf("sharp"), RotationPicker.candidates(list).map { it.id })
    }

    @Test
    fun candidates_preserves_order() {
        val list = listOf(wallpaper("b"), wallpaper("a"))
        assertEquals(listOf("b", "a"), RotationPicker.candidates(list).map { it.id })
    }

    @Test
    fun filterByCollection_null_returns_all() {
        val list = listOf(wallpaper("b"), wallpaper("a"))
        assertEquals(listOf("b", "a"), RotationPicker.filterByCollection(list, null).map { it.id })
    }

    @Test
    fun filterByCollection_empty_returns_empty() {
        val list = listOf(wallpaper("a"), wallpaper("b"))
        assertTrue(RotationPicker.filterByCollection(list, emptySet()).isEmpty())
    }

    @Test
    fun filterByCollection_subset_preserves_order() {
        val list = listOf(wallpaper("c"), wallpaper("a"), wallpaper("b"))
        val result = RotationPicker.filterByCollection(list, setOf("b", "c")).map { it.id }
        assertEquals(listOf("c", "b"), result)
    }

    @Test
    fun filterAvailable_uses_predicate() {
        val list = listOf(wallpaper("a"), wallpaper("b"), wallpaper("c"))
        val onDisk = mapOf("a" to true, "b" to false, "c" to true)
        val result = RotationPicker.filterAvailable(list) { onDisk[it] == true }.map { it.id }
        assertEquals(listOf("a", "c"), result)
    }

    @Test
    fun mapTarget_maps_all_values() {
        assertEquals(WallpaperPosition.HOME, RotationPicker.mapTarget(RotationTarget.HOME))
        assertEquals(WallpaperPosition.LOCK, RotationPicker.mapTarget(RotationTarget.LOCK))
        assertEquals(WallpaperPosition.BOTH, RotationPicker.mapTarget(RotationTarget.BOTH))
    }

    @Test
    fun retryOrder_wraps_around() {
        assertEquals(listOf(4, 0, 1), RotationPicker.retryOrder(4, 5))
    }

    @Test
    fun retryOrder_single_candidate_retries_same() {
        assertEquals(listOf(0, 0, 0), RotationPicker.retryOrder(0, 1))
    }

    @Test
    fun retryOrder_empty_is_empty() {
        assertTrue(RotationPicker.retryOrder(0, 0).isEmpty())
    }

    @Test
    fun shouldContinueChain_chain_off_stops() {
        assertFalse(RotationPicker.shouldContinueChain(true, RotationSchedule.OFF))
    }

    @Test
    fun shouldContinueChain_manual_off_runs() {
        assertTrue(RotationPicker.shouldContinueChain(false, RotationSchedule.OFF))
    }

    @Test
    fun shouldContinueChain_chain_active_continues() {
        assertTrue(RotationPicker.shouldContinueChain(true, RotationSchedule.DAILY))
        assertTrue(RotationPicker.shouldContinueChain(false, RotationSchedule.DAILY))
    }
}
