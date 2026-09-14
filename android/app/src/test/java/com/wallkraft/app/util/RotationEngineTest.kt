package com.wallkraft.app.util

import com.wallkraft.app.domain.model.Wallpaper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RotationEngineTest {

    private fun wallpaper(id: String) = Wallpaper(id = id)

    @Test
    fun pickNoRepeat_empty_candidates_returns_minus_one() {
        val result = RotationEngine.pickNoRepeat(emptyList(), emptyList())
        assertEquals(-1, result.index)
        assertEquals("", result.id)
    }

    @Test
    fun pickNoRepeat_single_candidate_always_picked() {
        val candidates = listOf(wallpaper("only-one"))
        repeat(20) {
            val result = RotationEngine.pickNoRepeat(candidates, emptyList(), seed = it.toLong())
            assertEquals(0, result.index)
            assertEquals("only-one", result.id)
        }
    }

    @Test
    fun pickNoRepeat_excludes_recents() {
        val candidates = listOf(wallpaper("a"), wallpaper("b"), wallpaper("c"))
        val recentIds = listOf("a", "c")
        repeat(30) {
            val result = RotationEngine.pickNoRepeat(candidates, recentIds, seed = it.toLong())
            assertNotEquals("a", result.id)
            assertNotEquals("c", result.id)
            assertEquals("b", result.id)
        }
    }

    @Test
    fun pickNoRepeat_all_recent_falls_back_to_full_pool() {
        val candidates = listOf(wallpaper("a"), wallpaper("b"))
        val recentIds = listOf("a", "b")
        val result = RotationEngine.pickNoRepeat(candidates, recentIds, seed = 42)
        assertTrue(result.id in listOf("a", "b"))
        assertTrue(result.index in 0..1)
    }

    @Test
    fun pickNoRepeat_seeded_determinism() {
        val candidates = listOf(wallpaper("x"), wallpaper("y"), wallpaper("z"))
        val recentIds = listOf("z")
        val r1 = RotationEngine.pickNoRepeat(candidates, recentIds, seed = 99)
        val r2 = RotationEngine.pickNoRepeat(candidates, recentIds, seed = 99)
        assertEquals(r1, r2)
    }

    @Test
    fun pickNoRepeat_no_immediate_repeat_over_many_picks() {
        val candidates = (0 until 5).map { wallpaper("w$it") }
        var recent = emptyList<String>()
        val shown = mutableListOf<String>()
        repeat(50) { i ->
            val result = RotationEngine.pickNoRepeat(candidates, recent, seed = i.toLong())
            if (shown.isNotEmpty()) {
                assertNotEquals(
                    "repeat at pick $i",
                    shown.last(),
                    result.id,
                )
            }
            shown.add(result.id)
            recent = (recent + result.id).takeLast(minOf(candidates.size - 1, 10))
        }
    }
}
