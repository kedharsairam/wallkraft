package com.wallkraft.app.util

import android.app.DownloadManager
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.wallkraft.app.R
import com.wallkraft.app.domain.model.Wallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Side-effect actions for a wallpaper: download to the Downloads folder,
 * set as wallpaper, open in browser, and share.
 */
object WallpaperActions {

    fun download(context: Context, wallpaper: Wallpaper): Long {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(wallpaper.path))
            .setTitle(context.getString(R.string.download_notification_title, wallpaper.id))
            .setDescription(wallpaper.resolution)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "WallKraft-${wallpaper.id}.jpg",
            )
        return dm.enqueue(request)
    }

    /**
     * Downloads the full-resolution image and applies it to the home and lock
     * screens. Suspends until done and returns true on success. Call from a
     * lifecycle-aware scope so the work is cancelled if the screen leaves.
     */
    suspend fun setAsWallpaper(
        context: Context,
        wallpaper: Wallpaper,
        client: OkHttpClient,
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(wallpaper.path).get().build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "HTTP ${response.code}" }
                val stream = response.body?.byteStream() ?: error("Empty body")
                WallpaperManager.getInstance(context).setStream(
                    stream,
                    null,
                    true,
                    WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK,
                )
            }
            true
        }.getOrDefault(false)
    }

    fun openInBrowser(context: Context, wallpaper: Wallpaper) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(wallpaper.url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun share(context: Context, wallpaper: Wallpaper) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "${wallpaper.resolution} — ${wallpaper.url}")
        }
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.share_title))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
