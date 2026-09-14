package com.wallkraft.app.data.cache

import android.util.Log
import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.Wallpaper
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
 *
 * Full per-wallpaper metadata is cached alongside search pages as
 * `wallpaper_<id>.json` files (see [getWallpaper]/[putWallpaper]). The two
 * namespaces share the directory but evict independently, so detail
 * metadata can never push out search pages or vice versa.
 */
open class SearchResponseCache(
    private val directory: File,
    private val json: Json,
) {
    private val ttlMillis = KraftConstants.SearchCacheTtlMs
    private val wallpaperTtlMillis = KraftConstants.WallpaperCacheTtlMs

    /** True when a fresh (within TTL) cached response exists. */
    open fun isFresh(filters: WallhavenFilters, page: Int): Boolean {
        val file = fileFor(filters, page)
        return file.exists() && System.currentTimeMillis() - file.lastModified() < ttlMillis
    }

    /** Returns the cached response for this (filters, page), or null. */
    open suspend fun get(filters: WallhavenFilters, page: Int): WallpaperResponse? =
        withContext(Dispatchers.IO) {
            val file = fileFor(filters, page)
            if (!file.exists()) return@withContext null
            runCatching {
                val decoded = json.decodeFromString(WallpaperResponse.serializer(), file.readText())
                // Old cache files predate cachedAt: fall back to the file's
                // mtime so stale indicators still work after upgrade.
                if (decoded.cachedAt == null) decoded.copy(cachedAt = file.lastModified()) else decoded
            }.getOrElse { e ->
                if (com.wallkraft.app.BuildConfig.DEBUG) Log.w("SearchResponseCache", "Failed to read cached response", e)
                null
            }
        }

    /** Stores a response for this (filters, page), evicting oldest entries. */
    open suspend fun put(filters: WallhavenFilters, page: Int, response: WallpaperResponse) =
        withContext(Dispatchers.IO) {
            runCatching {
                directory.mkdirs()
                val dest = fileFor(filters, page)
                val tmp = File(dest.parentFile, "${dest.name}.tmp")
                // Stamp wall-clock time so the UI can show "Updated X ago"
                // and [isFresh]/staleness checks have an authoritative value
                // beyond file.lastModified.
                val stamped = response.copy(cachedAt = System.currentTimeMillis())
                tmp.writeText(json.encodeToString(WallpaperResponse.serializer(), stamped))
                // Atomic replace — crash mid-write leaves dest intact.
                if (!tmp.renameTo(dest)) {
                    tmp.copyTo(dest, overwrite = true)
                    tmp.delete()
                }
                evictSearchIfNeeded()
            }.onFailure { e ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q || true) {
                    // Only log in debug — no PII, just cache hash.
                    if (com.wallkraft.app.BuildConfig.DEBUG) android.util.Log.w("SearchResponseCache", "Failed to cache search response", e)
                }
            }
        }

    /** True when a fresh (within the 7-day TTL) cached wallpaper exists. */
    open fun isWallpaperFresh(id: String): Boolean {
        val file = fileForWallpaper(id)
        return file.exists() && System.currentTimeMillis() - file.lastModified() < wallpaperTtlMillis
    }

    /**
     * Returns the cached wallpaper for [id], or null.
     * Stale entries are still returned — the repository uses this as an
     * offline fallback when the network fails, mirroring search behavior.
     */
    open suspend fun getWallpaper(id: String): Wallpaper? =
        withContext(Dispatchers.IO) {
            val file = fileForWallpaper(id)
            if (!file.exists()) return@withContext null
            runCatching {
                json.decodeFromString(Wallpaper.serializer(), file.readText())
            }.getOrElse { e ->
                if (com.wallkraft.app.BuildConfig.DEBUG) Log.w("SearchResponseCache", "Failed to read cached wallpaper", e)
                null
            }
        }

    /** Stores full wallpaper metadata for [wallpaper.id], evicting oldest entries. */
    open suspend fun putWallpaper(wallpaper: Wallpaper) =
        withContext(Dispatchers.IO) {
            runCatching {
                directory.mkdirs()
                val dest = fileForWallpaper(wallpaper.id)
                val tmp = File(dest.parentFile, "${dest.name}.tmp")
                tmp.writeText(json.encodeToString(Wallpaper.serializer(), wallpaper))
                // Atomic replace — crash mid-write leaves dest intact.
                if (!tmp.renameTo(dest)) {
                    tmp.copyTo(dest, overwrite = true)
                    tmp.delete()
                }
                evictWallpapersIfNeeded()
            }.onFailure { e ->
                if (com.wallkraft.app.BuildConfig.DEBUG) android.util.Log.w("SearchResponseCache", "Failed to cache wallpaper", e)
            }
        }

    private fun evictSearchIfNeeded() {
        val files = directory.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".json") && !it.name.startsWith(WALLPAPER_PREFIX) }
            ?: return
        if (files.size <= MAX_ENTRIES) return
        files.sortedBy { it.lastModified() }
            .take(files.size - MAX_ENTRIES)
            .forEach { it.delete() }
    }

    /** Bounds the wallpaper_*.json files (cap ~200, LRU by lastModified). */
    private fun evictWallpapersIfNeeded() {
        val files = directory.listFiles()
            ?.filter { it.isFile && it.name.startsWith(WALLPAPER_PREFIX) && !it.name.endsWith(".tmp") }
            ?: return
        if (files.size <= WALLPAPER_MAX_ENTRIES) return
        files.sortedBy { it.lastModified() }
            .take(files.size - WALLPAPER_MAX_ENTRIES)
            .forEach { it.delete() }
    }

    private fun fileForWallpaper(id: String): File {
        val safe = id.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
            .takeIf { it.isNotEmpty() } ?: id.hashCode().toString()
        return File(directory, "$WALLPAPER_PREFIX$safe.json")
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
        const val WALLPAPER_MAX_ENTRIES = KraftConstants.WallpaperCacheMaxEntries
        const val WALLPAPER_PREFIX = "wallpaper_"
        private val threadLocalDigest = ThreadLocal.withInitial { MessageDigest.getInstance("SHA-256") }
    }
}