package com.wallkraft.app

import android.content.Context
import androidx.room.Room
import com.wallkraft.app.data.api.WallhavenApi
import com.wallkraft.app.data.db.WallKraftDatabase
import com.wallkraft.app.data.prefs.SettingsStore
import com.wallkraft.app.data.repository.FavoritesRepositoryImpl
import com.wallkraft.app.data.repository.WallpaperRepositoryImpl
import com.wallkraft.app.domain.repository.FavoritesRepository
import com.wallkraft.app.domain.repository.SettingsRepository
import com.wallkraft.app.domain.repository.WallpaperRepository
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Manual dependency container. Keeps wiring explicit and lightweight —
 * no DI framework, per the Kraft principle of simplicity.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    val settings: SettingsRepository by lazy { SettingsStore(appContext) }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val wallhavenApi: WallhavenApi by lazy {
        WallhavenApi(okHttpClient, json, settings)
    }

    val wallpaperRepository: WallpaperRepository by lazy {
        WallpaperRepositoryImpl(wallhavenApi)
    }

    private val database: WallKraftDatabase by lazy {
        Room.databaseBuilder(appContext, WallKraftDatabase::class.java, "wallkraft.db")
            .build()
    }

    val favoritesRepository: FavoritesRepository by lazy {
        FavoritesRepositoryImpl(database.favoriteDao(), json)
    }
}
