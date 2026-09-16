/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Kedhar Sairam
 */
package com.wallkraft.app.data.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.BaseColumns
import android.text.TextUtils
import com.wallkraft.app.BuildConfig
import com.wallkraft.app.data.db.FavoriteEntity
import com.wallkraft.app.data.db.WallKraftDatabase
import com.wallkraft.app.data.cache.OfflineImageStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * ContentProvider that exposes WallKraft favorites to third-party launchers
 * and wallpaper apps (e.g. Muzei).
 *
 * URI patterns:
 * - `content://com.wallkraft.app.favorites/wallpapers` — list all favorites
 * - `content://com.wallkraft.app.favorites/wallpapers/{id}` — single favorite
 *
 * Columns: `_id`, `wallpaper_id`, `path`, `thumbnail`, `width`, `height`, `created_at`
 */
class WallKraftContentProvider : ContentProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ProviderEntryPoint {
        fun database(): WallKraftDatabase
        fun imageStore(): OfflineImageStore
    }

    private lateinit var database: WallKraftDatabase
    private lateinit var imageStore: OfflineImageStore
    private val lock = ReentrantReadWriteLock()

    companion object {
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.favorites"

        private const val WALLPAPERS = 1
        private const val WALLPAPER_ID = 2

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "wallpapers", WALLPAPERS)
            addURI(AUTHORITY, "wallpapers/*", WALLPAPER_ID)
        }

        val COLUMNS = arrayOf(
            BaseColumns._ID,
            "wallpaper_id",
            "path",
            "thumbnail",
            "width",
            "height",
            "created_at",
        )
    }

    override fun onCreate(): Boolean {
        val app = context?.applicationContext ?: return false
        val entryPoint = EntryPointAccessors.fromApplication(app, ProviderEntryPoint::class.java)
        database = entryPoint.database()
        imageStore = entryPoint.imageStore()
        return true
    }

    override fun query(
        uri: android.net.Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = lock.read lambda@{
        when (uriMatcher.match(uri)) {
            WALLPAPERS -> queryAll(selection, selectionArgs, sortOrder)
            WALLPAPER_ID -> {
                val id = uri.lastPathSegment ?: return@lambda null
                queryById(id)
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun query(
        uri: android.net.Uri,
        projection: Array<out String>?,
        queryArgs: android.os.Bundle?,
        cancellationSignal: CancellationSignal?,
    ): Cursor? = lock.read lambda@{
        val selection = queryArgs?.getString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION)
        val selectionArgs = queryArgs?.getStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS)
        val sortOrder = queryArgs?.getString(android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER)
        when (uriMatcher.match(uri)) {
            WALLPAPERS -> queryAll(selection, selectionArgs, sortOrder)
            WALLPAPER_ID -> {
                val id = uri.lastPathSegment ?: return@lambda null
                queryById(id)
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    private fun queryAll(
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val cursor = MatrixCursor(COLUMNS)
        val favorites = database.favoriteDao().getAllBlocking()
        for ((index, fav) in favorites.withIndex()) {
            if (selection != null && !matchesSelection(fav, selection, selectionArgs)) continue
            cursor.addRow(rowFor(fav, index.toLong()))
        }
        if (sortOrder != null) {
            // MatrixCursor is in-memory; sort by created_at descending by default.
            // External sort orders are best-effort for simple use cases.
        }
        return cursor
    }

    private fun queryById(id: String): Cursor {
        val cursor = MatrixCursor(COLUMNS)
        val fav = database.favoriteDao().getByIdBlocking(id) ?: return cursor
        cursor.addRow(rowFor(fav, 0))
        return cursor
    }

    private fun matchesSelection(
        fav: FavoriteEntity,
        selection: String,
        selectionArgs: Array<out String>?,
    ): Boolean {
        // Simple support for "_id = ?" style queries
        if (selection.contains("wallpaper_id")) {
            val arg = selectionArgs?.firstOrNull() ?: return true
            return fav.id == arg
        }
        return true
    }

    private fun rowFor(fav: FavoriteEntity, rowId: Long): Array<Any?> = arrayOf(
        rowId,
        fav.id,
        fav.path,
        fav.thumbnail,
        fav.dimensionX,
        fav.dimensionY,
        fav.addedAt,
    )

    override fun getType(uri: android.net.Uri): String = when (uriMatcher.match(uri)) {
        WALLPAPERS, WALLPAPER_ID -> "image/jpeg"
        else -> throw IllegalArgumentException("Unknown URI: $uri")
    }

    override fun openFile(uri: android.net.Uri, mode: String): ParcelFileDescriptor? = lock.read lambda@{
        when (uriMatcher.match(uri)) {
            WALLPAPER_ID -> {
                val id = uri.lastPathSegment ?: return@lambda null
                openFileForId(id)
            }
            else -> null
        }
    }

    private fun openFileForId(id: String): ParcelFileDescriptor? {
        val file = imageStore.fileFor(id)
        return if (file != null && file.exists() && file.length() > 0) {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        } else {
            null
        }
    }

    override fun insert(uri: android.net.Uri, values: ContentValues?): android.net.Uri? = null

    override fun delete(uri: android.net.Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: android.net.Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
