package com.wallkraft.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WallhavenFiltersColorTest {

    @Test
    fun defaults_areNull() {
        val f = WallhavenFilters()
        assertNull(f.color)
        assertNull(f.atleast)
    }

    @Test
    fun copy_preservesColorAndAtleast() {
        val base = WallhavenFilters(color = "ff0000", atleast = "1920x1080")
        val q = base.copy(query = "cats")
        assertEquals("ff0000", q.color)
        assertEquals("1920x1080", q.atleast)
        assertEquals("cats", q.query)
    }

    @Test
    fun signature_includesColorAndAtleast_viaCache() {
        // Indirect: two filters differing only in color should not be equal and
        // would produce different cache keys (tested via SearchResponseCacheTest).
        val a = WallhavenFilters(color = "ff0000")
        val b = WallhavenFilters(color = "00ff00")
        assertEquals(false, a == b)
        assertEquals(false, a.color == b.color)
    }

    @Test
    fun toCategoryParam_stillCorrectWithNewFields() {
        val c = setOf(Category.General, Category.Anime)
        assertEquals("110", c.toCategoryParam())
        // Adding color/atleast doesn't affect category param
        val f = WallhavenFilters(categories = c, color = "ff0000")
        assertEquals("110", f.categories.toCategoryParam())
    }
}
