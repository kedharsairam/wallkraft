package com.wallkraft.app.util

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.core.net.toUri
import com.wallkraft.app.R
import com.wallkraft.app.domain.model.Wallpaper

/**
 * Enqueues wallpaper downloads via the system DownloadManager.
 *
 * Split from the former WallpaperActions god object — this file owns
 * downloads and nothing else.
 */
object WallpaperDownload {

    private const val TAG = "WallpaperDownload"

    /**
     * Enqueues a download for [wallpaper] via the system DownloadManager.
     * Returns the download ID, or -1 on failure.
     */
    fun download(context: Context, wallpaper: Wallpaper): Long {
        return try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                ?: return -1L.also { Log.e(TAG, "DownloadManager unavailable") }
            // Detect actual file extension from the URL instead of hardcoding .jpg.
            val extension = wallpaper.path.toUri().lastPathSegment?.substringAfterLast('.', "jpg") ?: "jpg"
            val request = DownloadManager.Request(wallpaper.path.toUri())
                .setTitle(context.getString(R.string.download_notification_title, wallpaper.id))
                .setDescription(wallpaper.resolution)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "WallKraft-${wallpaper.id}.$extension",
                )
            dm.enqueue(request)
        } catch (e: SecurityException) {
            Log.e(TAG, "Storage permission denied for download", e)
            -1L
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue download", e)
            -1L
        }
    }
}
