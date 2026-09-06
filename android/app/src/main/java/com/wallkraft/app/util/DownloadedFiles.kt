package com.wallkraft.app.util

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.wallkraft.app.domain.model.DownloadedFile
import java.io.File

/**
 * Queries, deletes, and opens downloaded wallpaper files in the public
 * Downloads folder.
 *
 * Split from the former WallpaperActions god object — this file owns
 * downloaded-file bookkeeping and nothing else.
 */
object DownloadedFiles {

    /** Returns the set of IDs for wallpapers that have been downloaded to the device. */
    fun downloadedIds(context: Context): Set<String> =
        downloadedFiles(context).map { it.wallpaperId }.toSet()

    /** Returns the [DownloadedFile] for [wallpaperId], or null if not downloaded. */
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
}
