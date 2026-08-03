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

/**
 * Side-effect actions for a wallpaper: download to the Downloads folder,
 * set as wallpaper (with position selection), and open in browser.
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
     * Downloads the full-resolution image and applies it to the selected
     * screen(s). [position] controls which screens receive the wallpaper:
     * `WallpaperManager.FLAG_SYSTEM` for home, `FLAG_LOCK` for lock screen,
     * or both OR'd together.
     */
    suspend fun setAsWallpaper(
        context: Context,
        wallpaper: Wallpaper,
        client: OkHttpClient,
        position: WallpaperPosition = WallpaperPosition.BOTH,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(wallpaper.path).get().build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "HTTP ${response.code}" }
                val stream = response.body?.byteStream() ?: error("Empty body")
                WallpaperManager.getInstance(context).setStream(
                    stream,
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
