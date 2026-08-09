package com.wallkraft.app.util

import android.app.DownloadManager
import android.app.WallpaperManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import coil3.imageLoader
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

    /** Applies a pre-cropped [Bitmap] as the wallpaper at [position]. */
    fun setAsWallpaper(context: Context, bitmap: Bitmap, position: WallpaperPosition): Boolean =
        runCatching {
            WallpaperManager.getInstance(context).setBitmap(bitmap, null, true, position.flags)
        }.isSuccess

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
            val downloaded = downloadedFile(context, wallpaper.id)

            val stream = if (downloaded != null) {
                // Found in Downloads — use it directly.
                context.contentResolver.openInputStream(downloaded.uri)
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
                        // Temp file + rename so an interrupted download can't
                        // leave a partial file that looks valid next time.
                        val tmp = File(cacheFile.parentFile, "${wallpaper.id}.tmp")
                        tmp.outputStream().use { out -> body.byteStream().copyTo(out) }
                        if (!tmp.renameTo(cacheFile)) {
                            tmp.delete()
                            cacheFile.delete()
                            error("Failed to move downloaded image into place")
                        }
                    }
                    cacheFile.inputStream()
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

    /** Returns true if a wallpaper has been downloaded to the Downloads folder. */
    fun isDownloaded(context: Context, wallpaperId: String): Boolean =
        downloadedFile(context, wallpaperId) != null

    /** Returns the set of downloaded wallpaper IDs. */
    fun downloadedIds(context: Context): Set<String> =
        downloadedFiles(context).map { it.wallpaperId }.toSet()

    /** Returns the downloaded file for [wallpaperId], or null if not downloaded. */
    fun downloadedFile(context: Context, wallpaperId: String): DownloadedFile? =
        downloadedFiles(context).firstOrNull { it.wallpaperId == wallpaperId }

    /**
     * Lists the app's downloaded wallpapers in the public Downloads folder.
     *
     * API 29+ queries MediaStore.Downloads — the scoped-storage-correct way to
     * enumerate files in Downloads (raw path access is restricted there).
     * API 26-28 falls back to a raw directory scan, which is correct on those
     * versions because scoped storage doesn't exist yet.
     */
    fun downloadedFiles(context: Context): List<DownloadedFile> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            queryMediaStoreDownloads(context)
        } else {
            scanDownloadsDirectory(context)
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun queryMediaStoreDownloads(context: Context): List<DownloadedFile> {
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME,
            MediaStore.Downloads.SIZE,
            MediaStore.Downloads.RELATIVE_PATH,
        )
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("WallKraft-%")
        val result = mutableListOf<DownloadedFile>()
        context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.RELATIVE_PATH)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameCol) ?: continue
                val id = name.removePrefix("WallKraft-").substringBefore('.')
                if (id.isBlank()) continue
                val uri = ContentUris.withAppendedId(collection, cursor.getLong(idCol))
                result += DownloadedFile(
                    wallpaperId = id,
                    name = name,
                    size = cursor.getLong(sizeCol),
                    uri = uri,
                    relativePath = cursor.getString(pathCol) ?: "Download/",
                )
            }
        }
        return result
    }

    private fun scanDownloadsDirectory(context: Context): List<DownloadedFile> {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.name.startsWith("WallKraft-") && it.length() > 0 }
            ?.mapNotNull { file ->
                val id = file.name.removePrefix("WallKraft-").substringBefore('.')
                if (id.isBlank()) return@mapNotNull null
                DownloadedFile(
                    wallpaperId = id,
                    name = file.name,
                    size = file.length(),
                    uri = Uri.fromFile(file),
                    relativePath = "Download/",
                )
            }
            ?: emptyList()
    }

    /**
     * Deletes a downloaded wallpaper file from the device.
     *
     * API 29+ deletes through MediaStore (the scoped-storage-correct way —
     * the file was created by this app, so it owns it). API 26-28 deletes the
     * raw file directly, which is correct on those versions.
     */
    fun delete(context: Context, file: DownloadedFile): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { context.contentResolver.delete(file.uri, null, null) > 0 }
                .getOrDefault(false)
        } else {
            runCatching { file.uri.path?.let { File(it).delete() } == true }
                .getOrDefault(false)
        }

    /**
     * Opens the location of a downloaded file: the Downloads folder in the
     * system Files app (DocumentsUI). Falls back to opening the file itself
     * if no app can show the folder.
     */
    fun openDownloadLocation(context: Context, file: DownloadedFile) {
        val folderUri = DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents",
            "primary:Download",
        )
        val folderIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(folderUri, "vnd.android.document/root")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (runCatching { context.startActivity(folderIntent) }.isSuccess) return
        // Fallback: open the file itself.
        val fileIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(file.uri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(fileIntent) }
    }

    fun openInBrowser(context: Context, wallpaper: Wallpaper) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(wallpaper.url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
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
        client: OkHttpClient,
        localFile: File? = null,
    ): Boolean {
        val image = shareableFile(context, wallpaper, client, localFile)
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
        client: OkHttpClient,
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
        return downloadToCache(context, wallpaper, client)
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
        Uri.parse(wallpaper.path).lastPathSegment?.substringAfterLast('.', "jpg") ?: "jpg"

    /** Downloads [wallpaper]'s full-res into the cache dir, or returns null on failure. */
    private suspend fun downloadToCache(
        context: Context,
        wallpaper: Wallpaper,
        client: OkHttpClient,
    ): File? = withContext(Dispatchers.IO) {
        if (wallpaper.path.isBlank()) return@withContext null
        runCatching {
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            // Name the file with its real extension so FileProvider can resolve
            // the MIME type when the file is shared.
            val file = File(dir, "${wallpaper.id}.${extensionFor(wallpaper)}")
            if (file.exists() && file.length() > 0) return@runCatching file
            val request = Request.Builder().url(wallpaper.path).get().build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "HTTP ${response.code}" }
                val body = response.body ?: return@runCatching null
                // Temp file + rename so a failed download never leaves a
                // partial file that the length check above would accept.
                val tmp = File(dir, "${wallpaper.id}.tmp")
                tmp.outputStream().use { out -> body.byteStream().copyTo(out) }
                if (!tmp.renameTo(file)) {
                    tmp.delete()
                    file.delete()
                    error("Failed to move downloaded image into place")
                }
            }
            file
        }.getOrNull()
    }

    /**
     * Returns a local file for [wallpaper]'s image: the offline favorite copy
     * if present, otherwise downloaded into the cache. Null if unavailable.
     */
    suspend fun imageFile(
        context: Context,
        wallpaper: Wallpaper,
        client: OkHttpClient,
        localFile: File? = null,
    ): File? = localFile
        ?: coilCachedFile(context, wallpaper)
        ?: downloadToCache(context, wallpaper, client)
}

/** Which screen(s) to apply a wallpaper to. */
enum class WallpaperPosition(val flags: Int) {
    HOME(WallpaperManager.FLAG_SYSTEM),
    LOCK(WallpaperManager.FLAG_LOCK),
    BOTH(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK),
}

/** A wallpaper file the app has downloaded into the public Downloads folder. */
data class DownloadedFile(
    val wallpaperId: String,
    val name: String,
    val size: Long,
    val uri: Uri,
    val relativePath: String,
)
