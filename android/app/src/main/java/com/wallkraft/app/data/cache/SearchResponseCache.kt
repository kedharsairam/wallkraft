package com.wallkraft.app.data.cache

import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.WallpaperResponse
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
    fun get(filters: WallhavenFilters, page: Int): WallpaperResponse? {
        val file = fileFor(filters, page)
        if (!file.exists()) return null
        return runCatching {
            json.decodeFromString(WallpaperResponse.serializer(), file.readText())
        }.getOrNull()
    }

    /** Stores a response for this (filters, page), evicting oldest entries. */
    fun put(filters: WallhavenFilters, page: Int, response: WallpaperResponse) {
        runCatching {
            directory.mkdirs()
            fileFor(filters, page).writeText(json.encodeToString(WallpaperResponse.serializer(), response))
            evictIfNeeded()
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
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(directory, "$hash.json")
    }

    private fun WallhavenFilters.signature(): String =
        "${categories.map { it.name }.sorted()}|${sorting.value}|${orientation.value}|$query|${purity.apiValue}"

    private companion object {
        const val MAX_ENTRIES = KraftConstants.SearchCacheMaxEntries
    }
}