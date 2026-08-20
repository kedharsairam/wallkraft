package com.wallkraft.app.data.cache

import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.domain.model.Wallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * App-private, bounded storage for favorite wallpapers' full-res images.
 *
 * When a wallpaper is favorited, its full-resolution file is downloaded here so
 * it can be viewed with no internet connection. Files live in the app's private
 * files directory (not the evictable cache), so they survive restarts.
 * The store is bounded at 1GB (LRU by lastModified) to prevent unbounded growth
 * when a user favorites hundreds of wallpapers.
 *
 * Coil loads the local [File] directly, so the detail screen prefers this copy
 * when it exists and falls back to the network URL otherwise.
 */
class FavoriteImageStore(
    private val directory: File,
    private val client: OkHttpClient,
) {
    companion object {
        const val MAX_BYTES: Long = KraftConstants.FavoriteImageMaxBytes
    }

    /** The local file for [id], or null if it hasn't been downloaded. */
    fun fileFor(id: String): File? {
        val file = File(directory, id)
        return if (file.exists() && file.length() > 0) {
            // Touch for LRU ordering.
            file.setLastModified(System.currentTimeMillis())
            file
        } else null
    }

    /** Downloads the full-res image for [wallpaper] into the store. */
    suspend fun save(wallpaper: Wallpaper) {
        if (wallpaper.path.isBlank()) return
        val file = File(directory, wallpaper.id)
        if (file.exists() && file.length() > 0) return
        // Pre-check: don't start a download if we're already at the cap and the
        // device is low on storage (avoid filling the disk).
        if (isOverLimit()) evictOldest()
        withContext(Dispatchers.IO) {
            runCatching {
                directory.mkdirs()
                val request = Request.Builder().url(wallpaper.path).get().build()
                client.newCall(request).execute().use { response ->
                    check(response.isSuccessful) { "HTTP ${response.code}" }
                    val body = response.body ?: return@runCatching
                    // Write to a temp file and rename into place only on
                    // success, so a failed/interrupted download never leaves a
                    // partial file that `fileFor` would treat as valid.
                    val tmp = File(directory, "${wallpaper.id}.tmp")
                    tmp.outputStream().use { out -> body.byteStream().copyTo(out) }
                    if (!tmp.renameTo(file)) {
                        tmp.delete()
                        file.delete()
                        error("Failed to move downloaded image into place")
                    }
                    // Post-save: enforce bound.
                    if (isOverLimit()) evictOldest()
                }
            }
        }
    }

    /** Deletes the stored file for [id], if present. */
    fun delete(id: String) {
        fileFor(id)?.delete()
    }

    /** Total bytes of all favorite images. */
    fun totalBytes(): Long = directory.listFiles()?.sumOf { it.length() } ?: 0L

    private fun isOverLimit(): Boolean = totalBytes() > MAX_BYTES

    private fun evictOldest() {
        val files = directory.listFiles()?.filter { !it.name.endsWith(".tmp") } ?: return
        // Sort by lastModified (oldest first) — fileFor() touches on read so
        // recently viewed favorites are kept.
        val sorted = files.sortedBy { it.lastModified() }
        var total = totalBytes()
        for (f in sorted) {
            if (total <= MAX_BYTES) break
            val len = f.length()
            if (f.delete()) total -= len
        }
    }
}
