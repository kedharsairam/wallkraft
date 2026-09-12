package com.wallkraft.app.core.cache

import android.content.Context
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import com.wallkraft.app.core.design.KraftConstants
import okio.Path.Companion.toOkioPath

/**
 * Shared Coil cache configuration.
 *
 * Both the app's singleton loader (detail screen) and the grid loader reuse
 * the same [DiskCache] so a thumbnail fetched by the grid is available to the
 * detail screen (and vice-versa) without a second network round-trip.
 * The memory cache is also shared — both loaders reference the same instance
 * so the aggregate budget is 25%, not 50% (25% × 2 loaders would starve the app).
 *
 * Sizes are tuned for a wallpaper app: full-res images are several MB, so the
 * disk cache is generous (512 MB) to support offline favorites and instant
 * re-visits without eating the device's storage.
 *
 * Hilt migration: previously an `object`. Now an injectable `@Singleton` class
 * provided via [com.wallkraft.app.di.AppModule].
 */
@javax.inject.Singleton
class ImageCache @javax.inject.Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
) {

    @Volatile
    private var disk: DiskCache? = null

    @Volatile
    private var memory: MemoryCache? = null

    /** Shared disk cache, built once with the application context. */
    fun diskCache(): DiskCache {
        disk?.let { return it }
        synchronized(this) {
            if (disk == null) {
                disk = DiskCache.Builder()
                    .directory(context.cacheDir.resolve("coil").toOkioPath())
                    .maxSizeBytes(KraftConstants.CoilDiskMaxBytes)
                    .build()
            }
            return disk!!
        }
    }

    /** Shared memory cache: 25% of the app's memory budget, reused by all loaders. */
    fun memoryCache(): MemoryCache {
        memory?.let { return it }
        synchronized(this) {
            if (memory == null) {
                memory = MemoryCache.Builder()
                    .maxSizePercent(context, KraftConstants.CoilMemoryPercent)
                    .build()
            }
            return memory!!
        }
    }

    // Static fallback for legacy call sites that still use `ImageCache.memoryCache(context)`
    // during the transition (WallKraftApp, GridImageLoader fallback). Once all callers
    // are Hilt-injected this fallback can be removed.
    companion object {
        @Volatile
        private var fallback: ImageCache? = null

        private fun fallbackInstance(context: Context): ImageCache {
            fallback?.let { return it }
            synchronized(this) {
                if (fallback == null) {
                    fallback = ImageCache(context.applicationContext)
                }
                return fallback!!
            }
        }

        @JvmStatic
        fun diskCache(context: Context): DiskCache = fallbackInstance(context).diskCache()

        @JvmStatic
        fun memoryCache(context: Context): MemoryCache = fallbackInstance(context).memoryCache()
    }
}
