package com.wallkraft.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WallhavenFiltersColorTest {

    @Test
    fun defaults_areCorrect() {
        val f = WallhavenFilters()
        // Default: All categories (111), SFW only (100)
        assertEquals(setOf(Category.General, Category.Anime, Category.People), f.categories)
        assertEquals(setOf(Purity.SfW), f.purity)
        assertEquals("111", f.categories.toCategoryParam())
        assertEquals("100", f.purity.toPurityParam())
    }

    @Test
    fun copy_preservesPurity() {
        val base = WallhavenFilters(purity = setOf(Purity.SfW, Purity.Sketchy))
        val q = base.copy(query = "cats")
        assertEquals(setOf(Purity.SfW, Purity.Sketchy), q.purity)
        assertEquals("cats", q.query)
    }

    @Test
    fun purity_neverNsfw() {
        // Only 100 and 110 are allowed — never 001/011/111 that include NSFW.
        val s = setOf(Purity.SfW).toPurityParam()
        val ss = setOf(Purity.SfW, Purity.Sketchy).toPurityParam()
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
