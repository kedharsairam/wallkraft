/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Kedhar Sairam
 */
package com.wallkraft.app.domain.usecase

import android.content.Context
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.util.WallpaperDownload
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enqueues wallpaper downloads via the system DownloadManager.
 *
 * Thin use-case wrapper around [WallpaperDownload]. The class is injectable
 * and the download backend is replaceable for testing via [Downloader] interface.
 */
@Singleton
class DownloadWallpaperUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloader: Downloader = Downloader.Default,
) {

    /**
     * Abstraction over the download backend, extracted so unit tests
     * can supply a fake without touching the system DownloadManager.
     */
    interface Downloader {
        fun download(context: Context, wallpaper: Wallpaper): Long

        /** Production implementation that delegates to [WallpaperDownload]. */
        data object Default : Downloader {
            override fun download(context: Context, wallpaper: Wallpaper): Long =
                WallpaperDownload.download(context, wallpaper)
        }
    }

    /**
     * Enqueues a single wallpaper download.
     *
     * @return the download ID, or -1 on failure.
     */
    fun download(wallpaper: Wallpaper): Long =
        downloader.download(context, wallpaper)

    /**
     * Enqueues downloads for multiple wallpapers.
     *
     * @return a list of download IDs (each -1 on failure for that wallpaper).
     */
    fun downloadAll(wallpapers: List<Wallpaper>): List<Long> =
        wallpapers.map { downloader.download(context, it) }
}
