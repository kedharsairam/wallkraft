package com.wallkraft.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WallhavenFiltersColorTest {

    @Test
    fun defaults_areCorrect() {
        val f = WallhavenFilters()
        // Default: General only (100), SFW only (100)
        assertEquals(setOf(Category.General), f.categories)
        assertEquals(Purity.SfW, f.purity)
        assertEquals("100", f.categories.toCategoryParam())
        assertEquals("100", f.purity.apiValue)
    }

    @Test
    fun copy_preservesPurity() {
        val base = WallhavenFilters(purity = Purity.SfWSketchy)
        val q = base.copy(query = "cats")
        assertEquals(Purity.SfWSketchy, q.purity)
        assertEquals("cats", q.query)
    }

    @Test
    fun purity_neverNsfw() {
        // Only 100 and 110 are allowed — never 001/011/111 that include NSFW.
        val s = Purity.SfW.apiValue
        val ss = Purity.SfWSketchy.apiValue
        assertEquals("100", s)
        assertEquals("110", ss)
        // Ensure NSFW (001) is not an option
        assertEquals(false, s.contains("001") || ss.contains("001"))
    }

    @Test
    fun toCategoryParam_withGeneralOnly() {
        val c = setOf(Category.General)
        assertEquals("100", c.toCategoryParam())
        val f = WallhavenFilters(categories = c)
        assertEquals("100", f.categories.toCategoryParam())
    }
}
