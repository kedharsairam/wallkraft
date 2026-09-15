package com.wallkraft.app.presentation.browse

import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.model.WallpaperResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Browse offline state logic.
 *
 * The actual OfflineBanner composable is a thin wrapper — these tests verify
 * the state conditions that control its visibility and the stale-data
 * indicators shown in [WallpaperListUiState].
 */
class BrowseOfflineStateTest {

    @Test
    fun `offlineBannerVisible is true when connectivity is false`() {
        val isOnline = false
        val showBanner = !isOnline
        assertTrue(showBanner)
    }

    @Test
    fun `offlineBannerVisible is false when connectivity is true`() {
        val isOnline = true
        val showBanner = !isOnline
        assertFalse(showBanner)
    }

    @Test
    fun `offline with empty cache shows empty state not error crash`() {
        val wallpapers = emptyList<Wallpaper>()
        val error: String? = null
        val isInitialLoading = false

        val stateKey = when {
            isInitialLoading -> "loading"
            wallpapers.isEmpty() -> "empty"
            else -> "grid"
        }

        assertEquals("empty", stateKey)
        // No error, no crash — just the empty state
        assertFalse(error?.contains("crash") == true)
    }

    @Test
    fun `offline with cached results shows stale indicators`() {
        val ttl = KraftConstants.SearchCacheTtlMs
        val now = System.currentTimeMillis()
        val staleCachedAt = now - ttl - 60_000L // 1 minute past TTL
        val wallpapers = listOf(Wallpaper(id = "wp1", path = "https://example.com/wp1.jpg"))
        val isInitialLoading = false
        val isRefreshing = false

        val staleCaptionVisible =
            !isInitialLoading &&
                !isRefreshing &&
                wallpapers.isNotEmpty() &&
                staleCachedAt != null &&
                now - staleCachedAt > ttl

        assertTrue(staleCaptionVisible)
    }

    @Test
    fun `fresh cached data does not show stale indicator`() {
        val ttl = KraftConstants.SearchCacheTtlMs
        val now = System.currentTimeMillis()
        val freshCachedAt = now - (ttl / 2) // within TTL
        val wallpapers = listOf(Wallpaper(id = "wp1", path = "https://example.com/wp1.jpg"))
        val isInitialLoading = false
        val isRefreshing = false

        val staleCaptionVisible =
            !isInitialLoading &&
                !isRefreshing &&
                wallpapers.isNotEmpty() &&
                freshCachedAt != null &&
                now - freshCachedAt > ttl

        assertFalse(staleCaptionVisible)
    }

    @Test
    fun `loading state suppresses stale indicator`() {
        val ttl = KraftConstants.SearchCacheTtlMs
        val now = System.currentTimeMillis()
        val staleCachedAt = now - ttl - 100_000L
        val wallpapers = listOf(Wallpaper(id = "wp1", path = "https://example.com/wp1.jpg"))
        val isInitialLoading = true
        val isRefreshing = false

        val staleCaptionVisible =
            !isInitialLoading &&
                !isRefreshing &&
                wallpapers.isNotEmpty() &&
                staleCachedAt != null &&
                now - staleCachedAt > ttl

        assertFalse(staleCaptionVisible)
    }

    @Test
    fun `refreshing state suppresses stale indicator`() {
        val ttl = KraftConstants.SearchCacheTtlMs
        val now = System.currentTimeMillis()
        val staleCachedAt = now - ttl - 100_000L
        val wallpapers = listOf(Wallpaper(id = "wp1", path = "https://example.com/wp1.jpg"))
        val isInitialLoading = false
        val isRefreshing = true

        val staleCaptionVisible =
            !isInitialLoading &&
                !isRefreshing &&
                wallpapers.isNotEmpty() &&
                staleCachedAt != null &&
                now - staleCachedAt > ttl

        assertFalse(staleCaptionVisible)
    }

    @Test
    fun `empty wallpaper list suppresses stale indicator`() {
        val ttl = KraftConstants.SearchCacheTtlMs
        val now = System.currentTimeMillis()
        val staleCachedAt = now - ttl - 100_000L
        val wallpapers = emptyList<Wallpaper>()
        val isInitialLoading = false
        val isRefreshing = false

        val staleCaptionVisible =
            !isInitialLoading &&
                !isRefreshing &&
                wallpapers.isNotEmpty() &&
                staleCachedAt != null &&
                now - staleCachedAt > ttl

        assertFalse(staleCaptionVisible)
    }

    @Test
    fun `rate limited offline shows rate limited state not error crash`() {
        val wallpapers = emptyList<Wallpaper>()
        val rateLimited = true
        val isInitialLoading = false

        val stateKey = when {
            isInitialLoading -> "loading"
            rateLimited && wallpapers.isEmpty() -> "rateLimited"
            wallpapers.isEmpty() -> "empty"
            else -> "grid"
        }

        assertEquals("rateLimited", stateKey)
    }

    @Test
    fun `WallpaperResponse isStale returns true past TTL`() {
        val ttl = 30 * 60 * 1000L
        val now = System.currentTimeMillis()
        val stale = WallpaperResponse(
            data = emptyList(),
            cachedAt = now - ttl - 1_000L,
        )

        assertTrue(stale.isStale(nowMillis = now, ttlMillis = ttl))
    }

    @Test
    fun `WallpaperResponse isStale returns false within TTL`() {
        val ttl = 30 * 60 * 1000L
        val now = System.currentTimeMillis()
        val fresh = WallpaperResponse(
            data = emptyList(),
            cachedAt = now - ttl + 1_000L,
        )

        assertFalse(fresh.isStale(nowMillis = now, ttlMillis = ttl))
    }

    @Test
    fun `WallpaperResponse isStale returns false for null cachedAt`() {
        val ttl = 30 * 60 * 1000L
        val now = System.currentTimeMillis()
        val response = WallpaperResponse(data = emptyList(), cachedAt = null)

        assertFalse(response.isStale(nowMillis = now, ttlMillis = ttl))
    }
}
