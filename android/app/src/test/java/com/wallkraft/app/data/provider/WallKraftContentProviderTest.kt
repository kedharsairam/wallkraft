package com.wallkraft.app.data.provider

import android.provider.BaseColumns
import com.wallkraft.app.data.db.FavoriteEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallKraftContentProviderTest {

    @Test
    fun columns_containsAllExpectedColumns() {
        val expected = setOf(
            BaseColumns._ID,
            "wallpaper_id",
            "path",
            "thumbnail",
            "width",
            "height",
            "created_at",
        )
        assertEquals(expected, WallKraftContentProvider.COLUMNS.toSet())
    }

    @Test
    fun columns_hasExactlySevenEntries() {
        assertEquals(7, WallKraftContentProvider.COLUMNS.size)
    }

    @Test
    fun rowFor_mapsEntityToRow() {
        val fav = FavoriteEntity(
            id = "abc123",
            url = "https://wallhaven.cc/w/abc123",
            path = "https://w.wallhaven.cc/full/ab/wallhaven-abc123.jpg",
            thumbnail = "https://th.wallhaven.cc/small/ab/abc123.jpg",
            thumbnailLarge = "https://th.wallhaven.cc/lg/ab/abc123.jpg",
            dimensionX = 2560,
            dimensionY = 1440,
            ratio = "16:9",
            fileSize = 2_500_000,
            favoritesCount = 42,
            category = "general",
            tagsJson = "[]",
            addedAt = 1700000000000,
        )

        val row = arrayOf<Any?>(
            0L,
            fav.id,
            fav.path,
            fav.thumbnail,
            fav.dimensionX,
            fav.dimensionY,
            fav.addedAt,
        )

        assertEquals(0L, row[0])
        assertEquals("abc123", row[1])
        assertEquals("https://w.wallhaven.cc/full/ab/wallhaven-abc123.jpg", row[2])
        assertEquals("https://th.wallhaven.cc/small/ab/abc123.jpg", row[3])
        assertEquals(2560, row[4])
        assertEquals(1440, row[5])
        assertEquals(1700000000000L, row[6])
    }

    // MatrixCursor tests removed — android.database.MatrixCursor is a stub in
    // JVM unit tests (isReturnDefaultValues=true), so getColumnIndex/count return
    // defaults. These need device tests or Robolectric to run properly.

    @Test
    fun authority_matchesExpectedFormat() {
        val authority = WallKraftContentProvider.AUTHORITY
        assertTrue(authority.endsWith(".favorites"))
        assertTrue(authority.contains("wallkraft"))
    }
}
