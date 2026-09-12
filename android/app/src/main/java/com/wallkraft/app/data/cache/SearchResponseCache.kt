package com.wallkraft.app.data.cache

import android.util.Log
import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.WallpaperResponse
import com.wallkraft.app.domain.model.toPurityParam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

/**
 * Persistent, file-backed cache of Wallhaven search responses.
 *
 * Each (filters, page) combination is stored as a JSON file keyed by a hash of
 * the filters. Entries are considered fresh within [ttlMillis]; stale entries
 * are still returned as an offline fallback so Browse keeps working with no
 * network. The cache is bounded ([MAX_ENTRIES]) and evicts oldest-first.
 *
 * Search results are tagless, but that's fine to cache: the detail screen
 * always re-fetches the full wallpaper via `wallpaper(id)`, so a cached
 * tagless copy never shadows the full metadata.
 */
class SearchResponseCache(
    private val directory: File,
    private val json: Json,
) {
    private val ttlMillis = KraftConstants.SearchCacheTtlMs

    /** True when a fresh (within TTL) cached response exists. */
    fun isFresh(filters: WallhavenFilters, page: Int): Boolean {
        val file = fileFor(filters, page)
        return file.exists() && System.currentTimeMillis() - file.lastModified() < ttlMillis
    }

    /** Returns the cached response for this (filters, page), or null. */
    suspend fun get(filters: WallhavenFilters, page: Int): WallpaperResponse? =
        withContext(Dispatchers.IO) {
            val file = fileFor(filters, page)
            if (!file.exists()) return@withContext null
            runCatching {
                json.decodeFromString(WallpaperResponse.serializer(), file.readText())
            }.getOrNull()
        }

    /** Stores a response for this (filters, page), evicting oldest entries. */
    suspend fun put(filters: WallhavenFilters, page: Int, response: WallpaperResponse) =
        withContext(Dispatchers.IO) {
            runCatching {
                directory.mkdirs()
                val dest = fileFor(filters, page)
                val tmp = File(dest.parentFile, "${dest.name}.tmp")
                tmp.writeText(json.encodeToString(WallpaperResponse.serializer(), response))
                // Atomic replace — crash mid-write leaves dest intact.
                if (!tmp.renameTo(dest)) {
                    tmp.copyTo(dest, overwrite = true)
                    tmp.delete()
                }
                evictIfNeeded()
            }.onFailure { e ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q || true) {
                    // Only log in debug — no PII, just cache hash.
                    if (com.wallkraft.app.BuildConfig.DEBUG) android.util.Log.w("SearchResponseCache", "Failed to cache search response", e)
                }
            }
        }

    private fun evictIfNeeded() {
        val files = directory.listFiles() ?: return
        if (files.size <= MAX_ENTRIES) return
        files.sortedBy { it.lastModified() }
            .take(files.size - MAX_ENTRIES)
            .forEach { it.delete() }
    }

    private fun fileFor(filters: WallhavenFilters, page: Int): File {
        val key = "${filters.signature()}|$page"
        val hash = threadLocalDigest.get()!!.digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(directory, "$hash.json")
    }

    private fun WallhavenFilters.signature(): String =
        // v3: v2 omitted topRange — Month/Week with same query shared file.
        // Bump + include topRange so toplist ranges don't collide.
        "v3|${categories.map { it.name }.sorted()}|${sorting.value}|${topRange.value}|${orientation.value}|$query|${purity.toPurityParam()}|$colors"

    private companion object {
        const val MAX_ENTRIES = KraftConstants.SearchCacheMaxEntries
        private val threadLocalDigest = ThreadLocal.withInitial { MessageDigest.getInstance("SHA-256") }
    }
}