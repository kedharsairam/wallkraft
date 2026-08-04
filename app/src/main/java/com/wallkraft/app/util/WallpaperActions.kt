package com.wallkraft.app.util

import android.app.DownloadManager
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.wallkraft.app.R
import com.wallkraft.app.domain.model.Wallpaper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Side-effect actions for a wallpaper: download to the Downloads folder,
 * set as wallpaper (with position selection), and open in browser.
 */
object WallpaperActions {

    fun download(context: Context, wallpaper: Wallpaper): Long {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        // Detect actual file extension from the URL instead of hardcoding .jpg.
        val extension = Uri.parse(wallpaper.path).lastPathSegment?.substringAfterLast('.', "jpg") ?: "jpg"
        val request = DownloadManager.Request(Uri.parse(wallpaper.path))
            .setTitle(context.getString(R.string.download_notification_title, wallpaper.id))
            .setDescription(wallpaper.resolution)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "WallKraft-${wallpaper.id}.$extension",
            )
        return dm.enqueue(request)
    }

    /**
     * Sets the wallpaper. First checks if the image already exists in the
     * Downloads folder (from a previous download) to avoid re-downloading.
     * If not found, downloads it, caches it in the app's internal cache for
     * future reuse, and applies it.
     */
    suspend fun setAsWallpaper(
        context: Context,
        wallpaper: Wallpaper,
        client: OkHttpClient,
        position: WallpaperPosition = WallpaperPosition.BOTH,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Check if the wallpaper exists in the Downloads folder.
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val cachedFile = findCachedWallpaper(downloadsDir, wallpaper.id)

            val stream = if (cachedFile != null) {
                // Found in Downloads — use it directly.
                cachedFile.inputStream()
            } else {
                // 2. Not in Downloads — check internal cache.
                val cacheFile = File(context.cacheDir, "wallpapers/${wallpaper.id}")
                if (cacheFile.exists() && cacheFile.length() > 0) {
                    cacheFile.inputStream()
                } else {
                    // 3. Download and cache for future use.
                    cacheFile.parentFile?.mkdirs()
                    val request = Request.Builder().url(wallpaper.path).get().build()
                    client.newCall(request).execute().use { response ->
                        check(response.isSuccessful) { "HTTP ${response.code}" }
                        val body = response.body ?: error("Empty body")
                        // Write to cache file and return its stream.
                        cacheFile.outputStream().use { out -> body.byteStream().copyTo(out) }
                        cacheFile.inputStream()
                    }
                }
            }

            stream.use { s ->
                WallpaperManager.getInstance(context).setStream(
                    s,
                    null,
                    true,
                    position.flags,
                )
            }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    /** Searches the Downloads directory for a file matching `WallKraft-{id}.*`. */
    private fun findCachedWallpaper(dir: File, wallpaperId: String): File? {
        if (!dir.exists()) return null
        return dir.listFiles()?.firstOrNull { file ->
            file.name.startsWith("WallKraft-$wallpaperId.") && file.length() > 0
        }
    }

    /** Returns true if a wallpaper has been downloaded to the Downloads folder. */
    fun isDownloaded(context: Context, wallpaperId: String): Boolean {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return findCachedWallpaper(downloadsDir, wallpaperId) != null
    }

    /** Scans the Downloads folder and returns the set of downloaded wallpaper IDs. */
    fun downloadedIds(context: Context): Set<String> {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) return emptySet()
        return downloadsDir.listFiles()
            ?.filter { it.name.startsWith("WallKraft-") && it.length() > 0 }
            ?.mapNotNull { it.name.removePrefix("WallKraft-").substringBefore('.') }
            ?.toSet()
            ?: emptySet()
    }

    fun openInBrowser(context: Context, wallpaper: Wallpaper) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(wallpaper.url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/** Which screen(s) to apply a wallpaper to. */
enum class WallpaperPosition(val flags: Int) {
    HOME(WallpaperManager.FLAG_SYSTEM),
    LOCK(WallpaperManager.FLAG_LOCK),
    BOTH(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK),
}
