package com.wallkraft.app.data.cache

import com.wallkraft.app.domain.model.Category
import com.wallkraft.app.domain.model.Orientation
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.model.WallpaperMeta
import com.wallkraft.app.domain.model.WallpaperResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SearchResponseCacheTest {

    private lateinit var dir: File
    private lateinit var cache: SearchResponseCache
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("search_cache_test").toFile()
        cache = SearchResponseCache(dir, json)
    }

    private fun filters(
        query: String = "",
        color: String? = null,
        atleast: String? = null,
    ) = WallhavenFilters(
        categories = setOf(Category.General),
        sorting = Sorting.DateAdded,
        orientation = Orientation.Both,
        query = query,
        color = color,
        atleast = atleast,
    )

    private fun response(ids: List<String>) = WallpaperResponse(
        data = ids.map { Wallpaper(id = it, path = "https://example.com/$it.jpg") },
        meta = WallpaperMeta(currentPage = 1, lastPage = 1),
    )

    @Test
    fun putAndGet_roundTrip() {
        val f = filters("cats")
        val r = response(listOf("a", "b"))
        cache.put(f, 1, r)
        val out = cache.get(f, 1)
        assertNotNull(out)
        assertEquals(2, out!!.data.size)
        assertEquals("a", out.data[0].id)
    }

    @Test
    fun isFresh_trueWithinTtl_falseAfterTtl() {
        val f = filters("dogs")
        cache.put(f, 1, response(listOf("x")))
        assertTrue(cache.isFresh(f, 1))
        // Fake old file by setting lastModified to 31 minutes ago.
        val files = dir.listFiles()!!
        files[0].setLastModified(System.currentTimeMillis() - 31 * 60 * 1000L)
        assertFalse(cache.isFresh(f, 1))
        // Stale still returned as fallback.
        assertNotNull(cache.get(f, 1))
    }

    @Test
    fun get_returnsNullWhenMissing() {
        assertNull(cache.get(filters("missing"), 1))
    }

    @Test
    fun differentPage_isDifferentKey() {
        val f = filters("q")
        cache.put(f, 1, response(listOf("p1")))
        cache.put(f, 2, response(listOf("p2")))
        assertEquals("p1", cache.get(f, 1)!!.data[0].id)
        assertEquals("p2", cache.get(f, 2)!!.data[0].id)
    }

    @Test
    fun colorAndAtleast_arePartOfKey() {
        val base = filters(query = "q", color = null, atleast = null)
        val withColor = filters(query = "q", color = "ff0000", atleast = null)
        val withAtleast = filters(query = "q", color = null, atleast = "1920x1080")
        cache.put(base, 1, response(listOf("base")))
        assertNull(cache.get(withColor, 1))
        assertNull(cache.get(withAtleast, 1))
        cache.put(withColor, 1, response(listOf("red")))
        assertEquals("red", cache.get(withColor, 1)!!.data[0].id)
        assertEquals("base", cache.get(base, 1)!!.data[0].id)
    }

    @Test
    fun eviction_keepsAtMost100() {
        // Insert 101 entries, should evict oldest.
        for (i in 0 until 101) {
            cache.put(filters("q$i"), 1, response(listOf("id$i")))
            Thread.sleep(5) // ensure distinct mtime
        }
        val files = dir.listFiles()!!
        assertTrue(files.size <= 100)
    }
}
