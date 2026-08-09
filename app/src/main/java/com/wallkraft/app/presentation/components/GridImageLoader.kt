package com.wallkraft.app.presentation.components

import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.memory.MemoryCache
import coil3.request.crossfade

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
 */
object GridImageLoader {

    @Volatile
    private var loader: ImageLoader? = null

    /** Must be called once from app start with an application context. */
    fun init(context: Context) {
        if (loader != null) return
        synchronized(this) {
            if (loader == null) {
                loader = ImageLoader.Builder(context)
                    .crossfade(false)
                    .memoryCache { ImageCache.memoryCache(context) }
                    .diskCache { ImageCache.diskCache(context) }
                    .build()
            }
        }
    }

    fun get(): ImageLoader? = loader
}