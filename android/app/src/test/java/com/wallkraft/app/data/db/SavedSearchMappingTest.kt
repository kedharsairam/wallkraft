package com.wallkraft.app.data.db

import com.wallkraft.app.domain.model.Category
import com.wallkraft.app.domain.model.Orientation
import com.wallkraft.app.domain.model.Purity
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.domain.model.TopRange
import com.wallkraft.app.domain.model.WallhavenFilters
import org.junit.Assert.assertEquals
import org.junit.Test

class SavedSearchMappingTest {

    @Test
    fun round_trip_preserves_all_fields() {
        val filters = WallhavenFilters(
            categories = setOf(Category.Anime),
            sorting = Sorting.Toplist,
            topRange = TopRange.Year,
            orientation = Orientation.Landscape,
            query = "blue sky",
            purity = setOf(Purity.SFW, Purity.Sketchy),
            colors = "0000ff",
        )
        val entity = SavedSearchEntity.fromFilters("Blue anime", filters, now = 5000L)

        assertEquals("Blue anime", entity.name)
        assertEquals("blue sky", entity.query)
        assertEquals("010", entity.categories)
        assertEquals("110", entity.purity)
        assertEquals("toplist", entity.sorting)
        assertEquals("1y", entity.topRange)
        assertEquals("0000ff", entity.colors)
        assertEquals("landscape", entity.orientation)
        assertEquals(5000L, entity.createdAt)
        assertEquals(5000L, entity.lastUsedAt)
        assertEquals(0, entity.useCount)
        assertEquals(filters, entity.toFilters())
    }

    @Test
    fun round_trip_defaults() {
        val filters = WallhavenFilters()
        val entity = SavedSearchEntity.fromFilters("Fresh", filters, now = 1000L)

        assertEquals("111", entity.categories)
        assertEquals("100", entity.purity)
        assertEquals("date_added", entity.sorting)
        assertEquals("1M", entity.topRange)
        assertEquals("both", entity.orientation)
        assertEquals(filters, entity.toFilters())
    }

    @Test
    fun toplist_without_topRange_falls_back_to_month() {
        val entity = SavedSearchEntity(
            name = "Legacy toplist",
            query = "",
            categories = "111",
            purity = "100",
            sorting = "toplist",
            topRange = "",
            colors = "",
            orientation = "both",
            createdAt = 1000L,
            lastUsedAt = 1000L,
        )
        val filters = entity.toFilters()
        assertEquals(Sorting.Toplist, filters.sorting)
        assertEquals(TopRange.Month, filters.topRange)
    }

    @Test
    fun unknown_values_fall_back_to_defaults() {
        val entity = SavedSearchEntity(
            name = "Weird",
            query = "q",
            categories = "nope",
            purity = "xx",
            sorting = "shuffled",
            topRange = "2Q",
            colors = "",
            orientation = "diagonal",
            createdAt = 1000L,
            lastUsedAt = 1000L,
        )
        val filters = entity.toFilters()
        assertEquals(
            setOf(Category.General, Category.Anime, Category.People),
            filters.categories,
        )
        assertEquals(setOf(Purity.SFW), filters.purity)
        assertEquals(Sorting.DateAdded, filters.sorting)
        assertEquals(TopRange.Month, filters.topRange)
        assertEquals(Orientation.Both, filters.orientation)
    }

    @Test
    fun empty_categories_bitmask_stays_empty() {
        val entity = SavedSearchEntity(
            name = "None",
            query = "",
            categories = "000",
            purity = "100",
            sorting = "date_added",
            topRange = "1M",
            colors = "",
            orientation = "both",
            createdAt = 1000L,
            lastUsedAt = 1000L,
        )
        assertEquals(emptySet<Category>(), entity.toFilters().categories)
    }
}
