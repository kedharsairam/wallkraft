package com.wallkraft.app.di

import android.content.Context
import androidx.room.Room
import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.data.api.WallhavenApi
import com.wallkraft.app.data.cache.FavoriteImageStore
import com.wallkraft.app.data.cache.SearchResponseCache
import com.wallkraft.app.data.db.WallKraftDatabase
import com.wallkraft.app.data.prefs.SettingsStore
import com.wallkraft.app.data.repository.CollectionsRepositoryImpl
import com.wallkraft.app.data.repository.FavoritesRepositoryImpl
import com.wallkraft.app.data.repository.WallpaperRepositoryImpl
import com.wallkraft.app.domain.repository.CollectionsRepository
import com.wallkraft.app.domain.repository.FavoritesRepository
import com.wallkraft.app.domain.repository.SettingsRepository
import com.wallkraft.app.domain.repository.WallpaperRepository
import com.wallkraft.app.util.ElapsedClock
import com.wallkraft.app.util.toUserMessage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Hilt module — singleton graph for WallKraft.
 *
 * Pilot: Browse only via Hilt; the rest of the app still uses [com.wallkraft.app.AppContainer]
 * for a small blast radius. Over time, remaining screens/VMs will migrate and the manual
 * container will be removed. Keeping both alive is intentional for the transition (duplicate
 * Room/OkHttp instances are cheap and share the same files).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(KraftConstants.ConnectTimeoutSec, TimeUnit.SECONDS)
        .readTimeout(KraftConstants.ReadTimeoutSec, TimeUnit.SECONDS)
        .callTimeout(KraftConstants.CallTimeoutSec, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Provides @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context,
    ): SettingsRepository = SettingsStore(context)

    @Provides @Singleton
    fun provideWallhavenApi(
        client: OkHttpClient,
        json: Json,
        settings: SettingsRepository,
    ): WallhavenApi = WallhavenApi(client, json, settings)

    @Provides @Singleton
    fun provideSearchResponseCache(
        @ApplicationContext context: Context,
        json: Json,
    ): SearchResponseCache = SearchResponseCache(
        directory = File(context.cacheDir, "search_cache"),
        json = json,
    )

    @Provides @Singleton
    fun provideWallpaperRepository(
        api: WallhavenApi,
        cache: SearchResponseCache,
    ): WallpaperRepository = WallpaperRepositoryImpl(api, cache)

    @Provides @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): WallKraftDatabase = Room.databaseBuilder(
        context.applicationContext,
        WallKraftDatabase::class.java,
        "wallkraft.db",
    )
        .addMigrations(
            WallKraftDatabase.MIGRATION_1_2,
            WallKraftDatabase.MIGRATION_2_3,
            WallKraftDatabase.MIGRATION_3_4,
        )
        .build()

    @Provides @Singleton
    fun provideFavoritesRepository(
        db: WallKraftDatabase,
        json: Json,
    ): FavoritesRepository = FavoritesRepositoryImpl(db.favoriteDao(), json)

    @Provides @Singleton
    fun provideCollectionsRepository(
        db: WallKraftDatabase,
    ): CollectionsRepository = CollectionsRepositoryImpl(db.collectionDao())

    @Provides @Singleton
    fun provideFavoriteImageStore(
        @ApplicationContext context: Context,
        client: OkHttpClient,
    ): FavoriteImageStore = FavoriteImageStore(
        directory = File(context.filesDir, "favorites"),
        client = client,
    )

    @Provides @Singleton
    fun provideElapsedClock(): ElapsedClock =
        ElapsedClock { android.os.SystemClock.elapsedRealtime() }

    /**
     * Localized error mapper — UI shows R.string.* messages instead of raw API codes.
     * Provided as a function so ViewModels can stay agnostic of Resources.
     */
    @Provides @Singleton
    fun provideErrorMessageMapper(
        @ApplicationContext context: Context,
    ): (Throwable) -> String = { throwable ->
        throwable.toUserMessage(context.resources)
    }
}
