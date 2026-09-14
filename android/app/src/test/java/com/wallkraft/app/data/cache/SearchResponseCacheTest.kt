package com.wallkraft.app.data.cache

import com.wallkraft.app.domain.model.Category
import com.wallkraft.app.domain.model.Orientation
import com.wallkraft.app.domain.model.Purity
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
import kotlinx.coroutines.test.runTest
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
        purity: Set<Purity> = setOf(Purity.SFW),
    ) = WallhavenFilters(
        categories = setOf(Category.General),
        sorting = Sorting.DateAdded,
        orientation = Orientation.Both,
        query = query,
        purity = purity,
    )

    private fun response(ids: List<String>) = WallpaperResponse(
        data = ids.map { Wallpaper(id = it, path = "https://example.com/$it.jpg") },
        meta = WallpaperMeta(currentPage = 1, lastPage = 1),
    )

    @Test
    fun putAndGet_roundTrip() = runTest {
        val f = filters("cats")
        val r = response(listOf("a", "b"))
        cache.put(f, 1, r)
        val out = cache.get(f, 1)
        assertNotNull(out)
        assertEquals(2, out!!.data.size)
        assertEquals("a", out.data[0].id)
    }

    @Test
    fun isFresh_trueWithinTtl_falseAfterTtl() = runTest {
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
    fun get_returnsNullWhenMissing() = runTest {
        assertNull(cache.get(filters("missing"), 1))
    }

    @Test
    fun differentPage_isDifferentKey() = runTest {
        val f = filters("q")
        cache.put(f, 1, response(listOf("p1")))
        cache.put(f, 2, response(listOf("p2")))
        assertEquals("p1", cache.get(f, 1)!!.data[0].id)
        assertEquals("p2", cache.get(f, 2)!!.data[0].id)
    }

    @Test
    fun purity_isPartOfKey() = runTest {
        val base = filters(query = "q", purity = setOf(Purity.SFW))
        val withSketchy = filters(query = "q", purity = setOf(Purity.SFW, Purity.Sketchy))
        cache.put(base, 1, response(listOf("base")))
        assertNull(cache.get(withSketchy, 1))
        cache.put(withSketchy, 1, response(listOf("sketchy")))
        assertEquals("sketchy", cache.get(withSketchy, 1)!!.data[0].id)
        assertEquals("base", cache.get(base, 1)!!.data[0].id)
    }

    @Test
    fun colors_isPartOfKey() = runTest {
        val base = filters(query = "q")
        val blue = filters(query = "q").copy(colors = "0066cc")
        cache.put(base, 1, response(listOf("base")))
        assertNull(cache.get(blue, 1))
        cache.put(blue, 1, response(listOf("blue")))
        assertEquals("blue", cache.get(blue, 1)!!.data[0].id)
        assertEquals("base", cache.get(base, 1)!!.data[0].id)
    }

    @Test
    fun eviction_keepsAtMost100() = runTest {
        // Insert 101 entries, should evict oldest.
        for (i in 0 until 101) {
            cache.put(filters("q$i"), 1, response(listOf("id$i")))
            kotlinx.coroutines.delay(5) // ensure distinct mtime
        }
        val files = dir.listFiles()!!
        assertTrue(files.size <= 100)
    }

    @Test
    fun put_stampsCachedAt_andRoundTrips() = runTest {
        val f = filters("stamp")
        val before = System.currentTimeMillis()
        cache.put(f, 1, response(listOf("a")))
        val out = cache.get(f, 1)
        val after = System.currentTimeMillis()
        assertNotNull(out)
        assertNotNull(out!!.cachedAt)
        assertTrue(out.cachedAt!! >= before && out.cachedAt!! <= after)
        // Freshly written entries are not stale.
        assertFalse(out.isStale(nowMillis = after))
    }

    @Test
    fun get_preservesStaleCachedAt() = runTest {
        val f = filters("stale")
        cache.put(f, 1, response(listOf("a")))
        val file = dir.listFiles()!!.single()
        // Rewrite the stored payload with an old cachedAt (as if written 31min ago).
        val staleAt = System.currentTimeMillis() - 31 * 60 * 1000L
        val raw = file.readText()
        val decoded = json.decodeFromString(WallpaperResponse.serializer(), raw)
        file.writeText(json.encodeToString(WallpaperResponse.serializer(), decoded.copy(cachedAt = staleAt)))
        val out = cache.get(f, 1)
        assertNotNull(out)
        assertEquals(staleAt, out!!.cachedAt)
        assertTrue(out.isStale())
    }

    @Test
    fun isStale_ttlBoundary() {
        val ttl = 30 * 60 * 1000L
        val now = System.currentTimeMillis()
        // Null cachedAt (unstamped network data) is never stale.
        assertFalse(WallpaperResponse().isStale(nowMillis = now, ttlMillis = ttl))
        // Fresh within TTL (epsilon inside the boundary) is not stale.
        assertFalse(response(listOf("a")).copy(cachedAt = now - ttl + 1_000L).isStale(nowMillis = now, ttlMillis = ttl))
        // Just past the 30min TTL is stale.
        assertTrue(response(listOf("a")).copy(cachedAt = now - ttl - 1_000L).isStale(nowMillis = now, ttlMillis = ttl))
    }

    @Test
    fun wallpaper_putAndGet_roundTrip() = runTest {
        val wp = Wallpaper(id = "abc123", path = "https://example.com/a.jpg", views = 10, favorites = 5)
        cache.putWallpaper(wp)
        assertTrue(cache.isWallpaperFresh("abc123"))
        val out = cache.getWallpaper("abc123")
        assertNotNull(out)
        assertEquals("abc123", out!!.id)
        assertEquals(10, out.views)
        assertEquals(5, out.favorites)
    }

    @Test
    fun wallpaper_get_returnsNullWhenMissing() = runTest {
        assertNull(cache.getWallpaper("nope"))
        assertFalse(cache.isWallpaperFresh("nope"))
    }

    @Test
    fun wallpaper_isFresh_falseAfterTtl_butStaleStillReturned() = runTest {
        cache.putWallpaper(Wallpaper(id = "fresh1"))
        assertTrue(cache.isWallpaperFresh("fresh1"))
        // Fake an 8-day-old file: past the 7-day TTL.
        val file = dir.listFiles()!!.first { it.name.startsWith("wallpaper_") }
        file.setLastModified(System.currentTimeMillis() - 8 * 24 * 60 * 60 * 1000L)
        assertFalse(cache.isWallpaperFresh("fresh1"))
        // Stale still returned as the offline fallback.
        assertNotNull(cache.getWallpaper("fresh1"))
    }

    @Test
    fun wallpaper_eviction_keepsAtMost200() = runTest {
        for (i in 0 until 201) {
            cache.putWallpaper(Wallpaper(id = "wp$i"))
            kotlinx.coroutines.delay(2) // ensure distinct mtime
        }
        val wallpapers = dir.listFiles()!!.filter { it.name.startsWith("wallpaper_") && it.name.endsWith(".json") }
        assertTrue(wallpapers.size <= 200)
    }

    @Test
    fun wallpaper_doesNotEvictSearchEntries() = runTest {
        val f = filters("keep")
        cache.put(f, 1, response(listOf("kept")))
        for (i in 0 until 201) {
            cache.putWallpaper(Wallpaper(id = "wp$i"))
        }
        // Wallpaper pressure must never push out search pages.
        assertNotNull(cache.get(f, 1))
    }
}
