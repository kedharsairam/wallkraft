package com.wallkraft.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchHistoryTest {

    @Test
    fun add_prepends_query() {
        assertEquals(listOf("ocean"), SearchHistory.add(emptyList(), "ocean"))
        assertEquals(
            listOf("forest", "ocean"),
            SearchHistory.add(listOf("ocean"), "forest"),
        )
    }

    @Test
    fun add_ignores_blank() {
        assertEquals(listOf("ocean"), SearchHistory.add(listOf("ocean"), "   "))
    }

    @Test
    fun add_trims_whitespace() {
        assertEquals(listOf("ocean"), SearchHistory.add(emptyList(), "  ocean  "))
    }

    @Test
    fun add_dedupes_case_insensitively_keeping_latest() {
        assertEquals(
            listOf("Ocean", "forest"),
            SearchHistory.add(listOf("ocean", "forest"), "Ocean"),
        )
    }

    @Test
    fun add_bounds_length() {
        val full = (0 until SearchHistory.MAX_ENTRIES).map { "q$it" }
        val out = SearchHistory.add(full, "new")
        assertEquals(SearchHistory.MAX_ENTRIES, out.size)
        assertEquals("new", out.first())
    }

    @Test
    fun serialize_roundTrip() {
        val history = listOf("ocean waves", "mountains", "neon city")
        assertEquals(history, SearchHistory.deserialize(SearchHistory.serialize(history)))
    }

    @Test
    fun deserialize_empty_is_empty() {
        assertTrue(SearchHistory.deserialize("").isEmpty())
    }

    @Test
    fun deserialize_skips_blanks() {
        val raw = SearchHistory.serialize(listOf("a", "b"))
        assertEquals(listOf("a", "b"), SearchHistory.deserialize(raw))
    }
}
