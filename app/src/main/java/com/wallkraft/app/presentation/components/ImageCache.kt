package com.wallkraft.app.presentation.components

import android.content.Context
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import okio.Path.Companion.toOkioPath

/**
 * Shared Coil cache configuration.
 *
 * Both the app's singleton loader (detail screen) and the grid loader reuse
 * the same [DiskCache] so a thumbnail fetched by the grid is available to the
 * detail screen (and vice-versa) without a second network round-trip. Memory
 * caches are per-loader (each loader builds its own), which is Coil's default
 * and keeps the two loaders independent.
 *
 * Sizes are tuned for a wallpaper app: full-res images are several MB, so the
 * disk cache is generous (512 MB) to support offline favorites and instant
 * re-visits without eating the device's storage.
 */
object ImageCache {

    @Volatile
    private var disk: DiskCache? = null

    /** Shared disk cache, built once with the application context. */
    fun diskCache(context: Context): DiskCache {
        disk?.let { return it }
        synchronized(this) {
            if (disk == null) {
                disk = DiskCache.Builder()
                    .directory(context.cacheDir.resolve("coil").toOkioPath())
                    .maxSizeBytes(512L * 1024 * 1024) // 512 MB
                    .build()
            }
            return disk!!
        }
    }

    /** Per-loader memory cache: 25% of the app's memory budget. */
    fun memoryCache(context: Context): MemoryCache =
        MemoryCache.Builder()
            .maxSizePercent(context, 0.25)
            .build()
}