package com.wallkraft.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WallhavenFiltersTest {

    @Test
    fun categoryMaskMatchesApiFormat() {
        val all = setOf(Category.General, Category.Anime, Category.People)
        assertEquals("111", all.toCategoryParam())
        assertEquals("100", setOf(Category.General).toCategoryParam())
        assertEquals("010", setOf(Category.Anime).toCategoryParam())
        assertEquals("001", setOf(Category.People).toCategoryParam())
        assertEquals("110", setOf(Category.General, Category.Anime).toCategoryParam())
    }

    @Test
    fun purityMaskMatchesApiFormat() {
        assertEquals("100", setOf(Purity.Sfw).toPurityParam())
        assertEquals("110", setOf(Purity.Sfw, Purity.Sketchy).toPurityParam())
        assertEquals("111", setOf(Purity.Sfw, Purity.Sketchy, Purity.Nsfw).toPurityParam())
        assertEquals("001", setOf(Purity.Nsfw).toPurityParam())
    }

    @Test
    fun wallpaperFileSizeFormatting() {
        val small = Wallpaper(id = "a", fileSize = 512_000)
        assertEquals("500.0 KB", small.fileSizeFormatted())

        val big = Wallpaper(id = "b", fileSize = 3 * 1024 * 1024)
        assertEquals("3.0 MB", big.fileSizeFormatted())
    }
}
