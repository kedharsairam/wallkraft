package com.wallkraft.app.core.cache

import android.content.Context
import coil3.ImageLoader
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.wallkraft.app.BuildConfig

/**
 * Shared ImageLoader for grid thumbnails, configured WITHOUT crossfade.
 *
 * The app's singleton loader crossfades every image load. That's a nice touch
 * on the detail screen, but in the grid it makes every tile that scrolls into
 * view run a fade animation — per-frame compositing work that turns smooth
 * scrolling janky. Grid tiles pop in instantly instead. One instance is shared
 * by all cards so they also share Coil's memory cache. It shares the tuned
 * disk cache with the singleton loader (see [ImageCache]) so thumbnails and
 * full-res images are reused across screens.
 *
 * Hilt migration: previously an `object` with manual `init`. Now an injectable
 * `@Singleton` class provided via [com.wallkraft.app.di.AppModule]. Legacy
 * static accessors are retained for transition (they delegate to the injected
 * singleton via a global holder).
 */
@javax.inject.Singleton
class GridImageLoader @javax.inject.Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val imageCache: ImageCache,
) {

    @Volatile
    private var loader: ImageLoader? = null

    private fun getOrCreate(): ImageLoader {
        loader?.let { return it }
        synchronized(this) {
            if (loader == null) {
                loader = ImageLoader.Builder(context)
                    .crossfade(false)
                    .memoryCache { imageCache.memoryCache() }
                    .diskCache { imageCache.diskCache() }
                    .apply { if (BuildConfig.DEBUG) logger(DebugLogger()) }
                    .build()
                instance = this
            }
            return loader!!
        }
    }

    fun getLoader(): ImageLoader = getOrCreate()

    /** Instance init for Hilt consumers. */
    fun initLoader() {
        getOrCreate()
    }

    companion object {
        @Volatile
        private var instance: GridImageLoader? = null

        /** Static accessor for leaf composables that cannot inject directly. */
        fun get(): ImageLoader? = instance?.getLoader()

        /** Static init delegation for Application. */
        fun init(context: Context) {
            // If Hilt already created the singleton, this no-ops via instance check.
            // Otherwise, create a temporary holder for early startup before Hilt injects.
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        // Fallback manual creation (pre-Hilt) — will be replaced once Hilt provides the singleton.
                        val fallbackCache = ImageCache(context.applicationContext)
                        val fallback = GridImageLoader(context.applicationContext, fallbackCache)
                        fallback.getLoader()
                    }
                }
            }
        }
    }
}
