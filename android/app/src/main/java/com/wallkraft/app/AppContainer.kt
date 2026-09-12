package com.wallkraft.app

import android.content.Context
import android.content.res.Resources
import androidx.room.Room
import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.data.api.WallhavenApi
import com.wallkraft.app.data.cache.FavoriteImageStore
import com.wallkraft.app.data.cache.SearchResponseCache
import com.wallkraft.app.data.db.WallKraftDatabase
import com.wallkraft.app.data.prefs.SettingsStore
import com.wallkraft.app.data.prefs.SearchHistoryStore
import com.wallkraft.app.data.prefs.RotationStore
import com.wallkraft.app.data.prefs.RotationCropStore
import com.wallkraft.app.data.repository.FavoritesRepositoryImpl
import com.wallkraft.app.data.repository.CollectionsRepositoryImpl
import com.wallkraft.app.data.repository.WallpaperRepositoryImpl
import com.wallkraft.app.domain.repository.FavoritesRepository
import com.wallkraft.app.domain.repository.CollectionsRepository
import com.wallkraft.app.domain.repository.SettingsRepository
import com.wallkraft.app.domain.repository.WallpaperRepository
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Manual dependency container. Keeps wiring explicit and lightweight —
 * no DI framework, per the Kraft principle of simplicity.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** Application resources, for localized messages in ViewModels. */
    val resources: Resources get() = appContext.resources

    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    val settings: SettingsRepository by lazy { SettingsStore(appContext) }

    /** Wallpaper rotation schedule + cursor. */
    val rotation: RotationStore by lazy { RotationStore(appContext) }

    /** Per-wallpaper crop rects framed by the user. */
    val rotationCrops: RotationCropStore by lazy { RotationCropStore(appContext) }

    /** Recent search queries for suggestions. Separate store: history writes
        never re-emit the settings flow. */
    val searchHistory: SearchHistoryStore by lazy { SearchHistoryStore(appContext) }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(KraftConstants.ConnectTimeoutSec, TimeUnit.SECONDS)
            .readTimeout(KraftConstants.ReadTimeoutSec, TimeUnit.SECONDS)
            .callTimeout(KraftConstants.CallTimeoutSec, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    // Hilt now owns RateLimitState; AppContainer keeps a local instance for transition
    // so legacy manual DI still works (duplicate state is cheap, files are shared).
    private val rateLimitState: com.wallkraft.app.data.api.RateLimitState by lazy {
        com.wallkraft.app.data.api.RateLimitState()
    }

    private val wallhavenApi: WallhavenApi by lazy {
        WallhavenApi(okHttpClient, json, settings, rateLimitState)
    }

    /** Expose API for key validation in Settings. */
    val api: WallhavenApi get() = wallhavenApi

    /** File-backed cache of search responses (30-min TTL, offline fallback). */
    val searchCache: SearchResponseCache by lazy {
        SearchResponseCache(
            directory = File(appContext.cacheDir, "search_cache"),
            json = json,
        )
    }

    val wallpaperRepository: WallpaperRepository by lazy {
        WallpaperRepositoryImpl(wallhavenApi, searchCache)
    }

    private val database: WallKraftDatabase by lazy {
        Room.databaseBuilder(appContext, WallKraftDatabase::class.java, "wallkraft.db")
            .addMigrations(
                WallKraftDatabase.MIGRATION_1_2,
                WallKraftDatabase.MIGRATION_2_3,
                WallKraftDatabase.MIGRATION_3_4,
            )
            // Downgrade must not wipe user favorites/collections silently (P0).
            // Room will throw on downgrade instead — safe for sideloads.
            .build()
    }

    val favoritesRepository: FavoritesRepository by lazy {
        FavoritesRepositoryImpl(database.favoriteDao(), json)
    }

    val collectionsRepository: CollectionsRepository by lazy {
        CollectionsRepositoryImpl(database.collectionDao())
    }

    /** Non-evictable offline storage for favorite full-res images. */
    val favoriteImageStore: FavoriteImageStore by lazy {
        FavoriteImageStore(
            directory = File(appContext.filesDir, "favorites"),
            client = okHttpClient,
        )
    }
}
