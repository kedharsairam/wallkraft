package com.wallkraft.app.di

import android.content.Context
import androidx.room.Room
import com.wallkraft.app.core.cache.ImageCache
import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.data.api.GithubApi
import com.wallkraft.app.data.api.RetryInterceptor
import com.wallkraft.app.data.api.WallhavenApi
import com.wallkraft.app.data.cache.FavoriteImageStore
import com.wallkraft.app.data.cache.OfflineImageStore
import com.wallkraft.app.data.cache.SearchResponseCache
import com.wallkraft.app.data.db.WallKraftDatabase
import com.wallkraft.app.data.prefs.SettingsStore
import com.wallkraft.app.data.prefs.CropStore
import com.wallkraft.app.data.prefs.RotationCropStore
import com.wallkraft.app.data.prefs.RotationSettingsStore
import com.wallkraft.app.data.prefs.RotationStore
import com.wallkraft.app.data.prefs.SearchHistoryRepository
import com.wallkraft.app.data.prefs.SearchHistoryStore
import com.wallkraft.app.data.repository.CollectionsRepositoryImpl
import com.wallkraft.app.data.repository.FavoritesRepositoryImpl
import com.wallkraft.app.data.repository.SavedSearchRepositoryImpl
import com.wallkraft.app.data.repository.WallpaperRepositoryImpl
import com.wallkraft.app.data.wipe.AndroidWipeBackend
import com.wallkraft.app.domain.usecase.WipeBackend
import com.wallkraft.app.domain.repository.CollectionsRepository
import com.wallkraft.app.domain.repository.FavoritesRepository
import com.wallkraft.app.domain.repository.SavedSearchRepository
import com.wallkraft.app.domain.repository.SettingsRepository
import com.wallkraft.app.domain.repository.WallpaperRepository
import com.wallkraft.app.core.errors.AppError
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
 * All screens use Hilt for dependency injection.
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

    private fun baseOkHttpBuilder(): OkHttpClient.Builder = OkHttpClient.Builder()
        .connectTimeout(KraftConstants.ConnectTimeoutSec, TimeUnit.SECONDS)
        .readTimeout(KraftConstants.ReadTimeoutSec, TimeUnit.SECONDS)
        .callTimeout(KraftConstants.CallTimeoutSec, TimeUnit.SECONDS)

    @Provides @Singleton @WallhavenClient
    fun provideWallhavenClient(): OkHttpClient = baseOkHttpBuilder()
        .addInterceptor(RetryInterceptor())
        .retryOnConnectionFailure(false)
        .build()

    @Provides @Singleton @GithubClient
    fun provideGithubClient(): OkHttpClient = baseOkHttpBuilder()
        .retryOnConnectionFailure(false)
        .build()

    @Provides @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context,
    ): SettingsRepository = SettingsStore(context)

    @Provides @Singleton
    fun provideWallhavenApi(
        @WallhavenClient client: OkHttpClient,
        json: Json,
        settings: SettingsRepository,
        rateLimitState: com.wallkraft.app.data.api.RateLimitState,
    ): WallhavenApi = WallhavenApi(client, json, settings, rateLimitState)

    @Provides @Singleton
    fun provideGithubApi(
        @GithubClient client: OkHttpClient,
        json: Json,
    ): GithubApi = GithubApi(client, json)

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
            WallKraftDatabase.MIGRATION_4_5,
        )
        .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
        .build()

    @Provides @Singleton
    fun provideFavoritesRepository(
        db: WallKraftDatabase,
        json: Json,
        imageStore: OfflineImageStore,
    ): FavoritesRepository = FavoritesRepositoryImpl(db.favoriteDao(), json, imageStore)

    @Provides @Singleton
    fun provideCollectionsRepository(
        db: WallKraftDatabase,
    ): CollectionsRepository = CollectionsRepositoryImpl(db.collectionDao())

    @Provides @Singleton
    fun provideSavedSearchRepository(
        db: WallKraftDatabase,
    ): SavedSearchRepository = SavedSearchRepositoryImpl(db.savedSearchDao())

    @Provides @Singleton
    fun provideFavoriteImageStore(
        @ApplicationContext context: Context,
        @WallhavenClient client: OkHttpClient,
    ): OfflineImageStore = FavoriteImageStore(
        directory = File(context.filesDir, "favorites"),
        client = client,
    )

    @Provides @Singleton
    fun provideSearchHistoryStore(
        @ApplicationContext context: Context,
    ): SearchHistoryRepository = SearchHistoryStore(context)

    @Provides @Singleton
    fun provideRotationStore(
        @ApplicationContext context: Context,
    ): RotationSettingsStore = RotationStore(context)

    @Provides @Singleton
    fun provideRotationCropStore(
        @ApplicationContext context: Context,
    ): CropStore = RotationCropStore(context)

    @Provides @Singleton
    fun provideWipeBackend(
        @ApplicationContext context: Context,
        db: WallKraftDatabase,
        imageCache: ImageCache,
    ): WipeBackend = AndroidWipeBackend(context, db, imageCache)

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
    ): (AppError) -> String = { appError ->
        appError.toUserMessage(context.resources)
    }
}
