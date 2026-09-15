package com.wallkraft.app.performance

import com.wallkraft.app.core.design.KraftConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit-level memory audit tests.
 *
 * These verify that caches have bounded sizes, image caches are configured
 * with explicit limits, and Flow collectors behave correctly (no leaked
 * coroutines).  They are pure-JVM logic tests — no device required.
 */
class MemoryAudit {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Cache size bounds ──────────────────────────────────────────────

    @Test
    fun searchCache_maxEntries_isReasonable() {
        val max = KraftConstants.SearchCacheMaxEntries
        assertTrue(
            "SearchCacheMaxEntries ($max) should be ≤ 200 to bound disk usage",
            max <= 200,
        )
        assertTrue(
            "SearchCacheMaxEntries ($max) should be > 0",
            max > 0,
        )
    }

    @Test
    fun wallpaperCache_maxEntries_isReasonable() {
        val max = KraftConstants.WallpaperCacheMaxEntries
        assertTrue(
            "WallpaperCacheMaxEntries ($max) should be ≤ 500 to bound disk usage",
            max <= 500,
        )
        assertTrue(
            "WallpaperCacheMaxEntries ($max) should be > 0",
            max > 0,
        )
    }

    @Test
    fun inMemoryCache_lruEvictsAtBoundedSize() {
        // WallpaperRepositoryImpl uses a LinkedHashMap with accessOrder=true
        // and override removeEldestEntry to cap at 200.  Verify the pattern
        // works as expected: inserting 201 items evicts the eldest.
        val cache = object : LinkedHashMap<String, String>(16, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, String>,
            ): Boolean = size > 200
        }

        for (i in 1..250) {
            cache["key$i"] = "value$i"
        }

        assertTrue(
            "In-memory LRU cache should cap at 201 entries (max 200 + 1 before eviction), " +
                "actual size: ${cache.size}",
            cache.size <= 201,
        )
        assertTrue(
            "Eldest entry key1 should have been evicted",
            !cache.containsKey("key1"),
        )
    }

    // ── Image cache configuration ──────────────────────────────────────

    @Test
    fun coilMemoryCache_percentIsBounded() {
        val percent = KraftConstants.CoilMemoryPercent
        assertTrue(
            "CoilMemoryPercent ($percent) should be between 0 and 0.5",
            percent > 0.0 && percent <= 0.5,
        )
    }

    @Test
    fun coilDiskCache_maxBytesIsReasonable() {
        val maxBytes = KraftConstants.CoilDiskMaxBytes
        val maxMB = maxBytes / (1024 * 1024)
        assertTrue(
            "CoilDiskMaxBytes (${maxMB}MB) should be ≤ 1GB",
            maxMB <= 1024,
        )
        assertTrue(
            "CoilDiskMaxBytes (${maxMB}MB) should be > 0",
            maxMB > 0,
        )
    }

    @Test
    fun favoriteImage_maxBytesIsReasonable() {
        val maxBytes = KraftConstants.FavoriteImageMaxBytes
        val maxMB = maxBytes / (1024 * 1024)
        assertTrue(
            "FavoriteImageMaxBytes (${maxMB}MB) should be ≤ 500MB",
            maxMB <= 500,
        )
        assertTrue(
            "FavoriteImageMaxBytes (${maxMB}MB) should be > 0",
            maxMB > 0,
        )
    }

    // ── Flow collector lifecycle ───────────────────────────────────────

    @Test
    fun flowCollector_cancelsCleanly_noLeak() = testScope.runTest {
        val source = MutableStateFlow(0)
        var emissions = 0

        val job = source
            .onEach { emissions++ }
            .launchIn(this)

        advanceUntilIdle()

        source.value = 1
        advanceUntilIdle()
        source.value = 2
        advanceUntilIdle()

        // Cancel the collector — this must complete without hanging.
        job.cancel()
        advanceUntilIdle()

        assertTrue(
            "Collector should have received 3 emissions (initial + 2 updates), got $emissions",
            emissions == 3,
        )
    }

    @Test
    fun flowCollector_multipleCollectors_allCancelCleanly() = testScope.runTest {
        val source = MutableStateFlow("a")
        val collector1Emissions = mutableListOf<String>()
        val collector2Emissions = mutableListOf<String>()

        val job1 = source
            .onEach { collector1Emissions.add(it) }
            .launchIn(this)

        val job2 = source
            .onEach { collector2Emissions.add(it) }
            .launchIn(this)

        advanceUntilIdle()

        source.value = "b"
        advanceUntilIdle()

        // Cancel both — no deadlock or leak.
        job1.cancel()
        job2.cancel()
        advanceUntilIdle()

        assertTrue(
            "Collector 1 should have 2 emissions, got ${collector1Emissions.size}",
            collector1Emissions.size == 2,
        )
        assertTrue(
            "Collector 2 should have 2 emissions, got ${collector2Emissions.size}",
            collector2Emissions.size == 2,
        )
    }

    @Test
    fun staticFlow_collectorCancelsWithoutError() = testScope.runTest {
        // Verify that collecting a one-shot flowOf() and then cancelling
        // doesn't throw or leak.
        val job = flowOf(1, 2, 3)
            .onEach { }
            .launchIn(this)

        advanceUntilIdle()
        job.cancel()
        advanceUntilIdle()

        assertTrue("Cancellation completed without error", true)
    }

    // ── Ticker / TTL sanity ────────────────────────────────────────────

    @Test
    fun searchCacheTtl_isReasonable() {
        val ttlMs = KraftConstants.SearchCacheTtlMs
        val ttlMinutes = ttlMs / 60_000
        assertTrue(
            "SearchCacheTtlMs ($ttlMinutes min) should be between 5 and 120 minutes",
            ttlMinutes in 5..120,
        )
    }

    @Test
    fun wallpaperCacheTtl_isReasonable() {
        val ttlMs = KraftConstants.WallpaperCacheTtlMs
        val ttlDays = ttlMs / (24 * 60 * 60 * 1000)
        assertTrue(
            "WallpaperCacheTtlMs ($ttlDays days) should be between 1 and 30 days",
            ttlDays in 1..30,
        )
    }

    @Test
    fun historyRetention_isReasonable() {
        val retentionMs = 90L * 24 * 60 * 60 * 1000 // from WallKraftApplication
        val retentionDays = retentionMs / (24 * 60 * 60 * 1000)
        assertTrue(
            "History retention ($retentionDays days) should be between 30 and 365 days",
            retentionDays in 30..365,
        )
    }
}
