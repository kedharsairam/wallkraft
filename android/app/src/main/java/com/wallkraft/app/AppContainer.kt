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
import com.wallkraft.app.data.repository.FavoritesRepositoryImpl
import com.wallkraft.app.data.repository.WallpaperRepositoryImpl
import com.wallkraft.app.domain.repository.FavoritesRepository
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

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(KraftConstants.ConnectTimeoutSec, TimeUnit.SECONDS)
            .readTimeout(KraftConstants.ReadTimeoutSec, TimeUnit.SECONDS)
            .callTimeout(KraftConstants.CallTimeoutSec, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val wallhavenApi: WallhavenApi by lazy {
        WallhavenApi(okHttpClient, json, settings)
    }

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
            .addMigrations(WallKraftDatabase.MIGRATION_1_2, WallKraftDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    val favoritesRepository: FavoritesRepository by lazy {
        FavoritesRepositoryImpl(database.favoriteDao(), json)
    }

    /** Non-evictable offline storage for favorite full-res images. */
    val favoriteImageStore: FavoriteImageStore by lazy {
        FavoriteImageStore(
            directory = File(appContext.filesDir, "favorites"),
            client = okHttpClient,
        )
    }
}
