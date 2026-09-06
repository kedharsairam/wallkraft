package com.wallkraft.app.util

import com.wallkraft.app.domain.model.CropRect
import com.wallkraft.app.domain.model.RotationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RotationFramingTest {

    // Screen 1080x2400 (aspect 0.45).
    private val screenW = 1080
    private val screenH = 2400

    @Test
    fun fill_portrait_centers_sides() {
        // 1080x2400 source on a 1080x2400 screen: full frame.
        val rect = RotationFraming.frameRect(1080, 2400, screenW, screenH, null, RotationMode.FILL)
        assertEquals(CropRect(0f, 0f, 1f, 1f), rect)
    }

    @Test
    fun fill_landscape_trims_sides() {
        // 21:9 ultrawide (aspect ~2.37) on 0.45 screen: narrow center slice.
        val rect = RotationFraming.frameRect(2560, 1080, screenW, screenH, null, RotationMode.FILL)
        assertTrue(rect.left > 0.3f)
        assertEquals(0f, rect.top)
        assertEquals(1f, rect.bottom)
        // True aspect of the slice matches the screen.
        assertEquals(0.45f, rect.aspect(2560f / 1080f), 0.01f)
    }

    @Test
    fun fill_square_trims_sides() {
        // Square (aspect 1.0) is wider than the 0.45 screen: trim the sides.
        val rect = RotationFraming.frameRect(1080, 1080, screenW, screenH, null, RotationMode.FILL)
        assertEquals(0.275f, rect.left, 0.001f)
        assertEquals(0f, rect.top)
        assertEquals(1f, rect.bottom)
        assertEquals(0.45f, rect.aspect(1f), 0.01f)
    }

    @Test
    fun saved_crop_wins_when_matching() {
        // Normalized width 0.19 of a 2.37-aspect source = true aspect 0.45.
        val saved = CropRect(0.4f, 0f, 0.59f, 1f)
        val rect = RotationFraming.frameRect(2560, 1080, screenW, screenH, saved, RotationMode.FILL)
        assertEquals(saved, rect)
    }

    @Test
    fun saved_crop_ignored_when_wrong_aspect() {
        // Full-square saved crop on a tall screen: falls back to center crop
        // (sides trimmed for the square source).
        val saved = CropRect(0f, 0f, 1f, 1f)
        val rect = RotationFraming.frameRect(1080, 1080, screenW, screenH, saved, RotationMode.FILL)
        assertTrue(rect.left > 0f)
    }

    @Test
    fun invalid_saved_crop_ignored() {
        val saved = CropRect(0.9f, 0.9f, 0.91f, 0.91f) // sliver
        val rect = RotationFraming.frameRect(1920, 1080, screenW, screenH, saved, RotationMode.FILL)
        assertEquals(0f, rect.top)
    }

    @Test
    fun showcase_and_atmosphere_use_full_image() {
        for (mode in listOf(RotationMode.SHOWCASE, RotationMode.ATMOSPHERE)) {
            assertEquals(
                CropRect(0f, 0f, 1f, 1f),
                RotationFraming.frameRect(2560, 1080, screenW, screenH, null, mode),
            )
        }
    }

    @Test
    fun degenerate_sizes_fall_back_to_full() {
        assertEquals(
            CropRect(0f, 0f, 1f, 1f),
            RotationFraming.frameRect(0, 0, screenW, screenH, null, RotationMode.FILL),
        )
    }

    @Test
    fun crop_rect_validity() {
        assertTrue(CropRect(0f, 0f, 1f, 1f).isValid())
        assertFalse(CropRect(0.5f, 0.5f, 0.51f, 0.51f).isValid())
        assertFalse(CropRect(-0.1f, 0f, 1f, 1f).isValid())
        assertFalse(CropRect(0.6f, 0f, 0.4f, 1f).isValid())
    }

    @Test
    fun crops_codec_round_trip_and_corrupt() {
        val map = mapOf("a" to CropRect(0.1f, 0.2f, 0.8f, 0.9f))
        assertEquals(map, RotationCrops.decode(RotationCrops.encode(map)))
        assertTrue(RotationCrops.decode("").isEmpty())
        assertTrue(RotationCrops.decode("not-json{{{").isEmpty())
    }

    @Test
    fun crops_codec_default_rect_round_trip() {
        // All-default values must survive (self-describing payload).
        val map = mapOf("a" to CropRect(0f, 0f, 1f, 1f))
        val encoded = RotationCrops.encode(map)
        assertTrue(encoded.contains("\"l\""))
        assertEquals(map, RotationCrops.decode(encoded))
    }
}
