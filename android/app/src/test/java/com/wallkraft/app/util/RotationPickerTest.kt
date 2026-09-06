package com.wallkraft.app.util

import com.wallkraft.app.domain.model.Wallpaper
import org.junit.Assert.assertEquals
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
}
