package com.wallkraft.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Size
import com.wallkraft.app.domain.model.Wallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Shares wallpapers and resolves local image files for sharing, cropping,
 * and wallpaper setting.
 *
 * Split from the former WallpaperActions god object — this file owns
 * sharing and local-file resolution and nothing else.
 */
object WallpaperSharing {

    fun openInBrowser(context: Context, wallpaper: Wallpaper) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, wallpaper.url.toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    /**
     * Shares a wallpaper. Prefers sharing the actual image: uses [localFile]
     * (e.g. the offline favorite copy) if present, otherwise downloads the
     * full-res into the cache and shares that. If the image can't be obtained,
     * falls back to sharing the wallhaven.cc URL as text. Returns true if a
     * share intent was launched.
     */
    suspend fun share(
        context: Context,
        wallpaper: Wallpaper,
        localFile: File? = null,
    ): Boolean {
        val image = shareableFile(context, wallpaper, localFile)
        if (image != null) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                image,
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, null))
            return true
        }
        // Fallback: share the link.
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, wallpaper.url)
        }
        context.startActivity(Intent.createChooser(intent, null))
        return true
    }

    /**
     * Resolves the file to share. The receiving app (WhatsApp, etc.) derives
     * the content type from the file extension via the FileProvider, so the
     * file MUST carry a real extension — an extensionless file comes back as
     * `application/octet-stream` and lands as a `.bin` on the other end.
     *
     * Prefers, in order:
     * 1. The offline favorite copy (already on disk, no network).
     * 2. The full-res file Coil has already downloaded for the detail screen
     *    (its disk cache is keyed by URL) — copied locally, no second download.
     * 3. A fresh download, only when the image isn't cached yet.
     */
    private suspend fun shareableFile(
        context: Context,
        wallpaper: Wallpaper,
        localFile: File?,
    ): File? {
        val ext = extensionFor(wallpaper)
        val local = localFile?.takeIf { it.exists() && it.length() > 0 }
        if (local != null) {
            if (local.name.contains('.')) return local
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            val named = File(dir, "${wallpaper.id}.$ext")
            if (!named.exists() || named.length() == 0L) {
                runCatching { local.copyTo(named, overwrite = true) }
            }
            return named.takeIf { it.exists() && it.length() > 0 } ?: local
        }
        coilCachedFile(context, wallpaper)?.let { return it }
        return coilFetchToCache(context, wallpaper)
    }

    /**
     * Returns a shareable copy of the full-res image Coil has already cached
     * for [wallpaper], or null if it isn't in Coil's disk cache yet.
     *
     * The detail screen loads the full-res through Coil, which stores the raw
     * image in its disk cache keyed by the URL. Reusing that file means sharing
     * costs zero extra data — the image was already downloaded for display.
     */
    private suspend fun coilCachedFile(context: Context, wallpaper: Wallpaper): File? =
        withContext(Dispatchers.IO) {
            if (wallpaper.path.isBlank()) return@withContext null
            runCatching {
                val snapshot = context.imageLoader.diskCache
                    ?.openSnapshot(wallpaper.path)
                    ?: return@runCatching null
                snapshot.use { snap ->
                    val data = snap.data.toFile()
                    if (!data.exists() || data.length() == 0L) return@use null
                    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
                    val file = File(dir, "${wallpaper.id}.${extensionFor(wallpaper)}")
                    if (!file.exists() || file.length() == 0L) {
                        data.copyTo(file, overwrite = true)
                    }
                    file.takeIf { it.exists() && it.length() > 0 }
                }
            }.getOrNull()
        }

    /** The file extension for [wallpaper]'s image, from its URL (defaults to jpg). */
    private fun extensionFor(wallpaper: Wallpaper): String =
        wallpaper.path.toUri().lastPathSegment?.substringAfterLast('.', "jpg") ?: "jpg"

    /**
     * Ensures [wallpaper]'s full-res image is in Coil's disk cache, then returns
     * a shareable copy of it (or null on failure).
     *
     * The request mirrors the detail screen's exactly — same data, decoded at
     * [Size.ORIGINAL], on the app's singleton loader — so if the detail screen
     * is already loading the full-res, Coil joins that in-flight request
     * instead of starting a second download. The memory cache is disabled
     * because we only need the bytes on disk; the fetch still populates the
     * disk cache, which [coilCachedFile] then reads.
     */
    private suspend fun coilFetchToCache(context: Context, wallpaper: Wallpaper): File? =
        withContext(Dispatchers.IO) {
            if (wallpaper.path.isBlank()) return@withContext null
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(wallpaper.path)
                    .size(Size.ORIGINAL)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
                context.imageLoader.execute(request)
                coilCachedFile(context, wallpaper)
            }.getOrNull()
        }

    /**
     * Returns a local file for [wallpaper]'s image: the offline favorite copy
     * if present, otherwise downloaded into the cache. Null if unavailable.
     */
    suspend fun imageFile(
        context: Context,
        wallpaper: Wallpaper,
        localFile: File? = null,
    ): File? = localFile
        ?: coilCachedFile(context, wallpaper)
        ?: coilFetchToCache(context, wallpaper)
}
