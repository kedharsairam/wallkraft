package com.wallkraft.app.data.wipe

import android.content.Context
import com.wallkraft.app.core.cache.ImageCache
import com.wallkraft.app.data.db.WallKraftDatabase
import com.wallkraft.app.data.prefs.EncryptedApiKeyStore
import com.wallkraft.app.data.prefs.RotationCropStore
import com.wallkraft.app.data.prefs.RotationStore
import com.wallkraft.app.data.prefs.SearchHistoryStore
import com.wallkraft.app.data.prefs.SettingsStore
import com.wallkraft.app.data.rotation.RotationScheduler
import com.wallkraft.app.domain.model.RotationSchedule
import com.wallkraft.app.domain.usecase.WipeBackend
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production [WipeBackend]: wipes every storage area WallKraft owns.
 *
 * Inventory (keep in sync when adding storage):
 * - WorkManager unique chain `wallkraft-rotation-chain` (+ legacy
 *   `wallkraft-rotation` rolling work) via [RotationScheduler].
 * - Room database `wallkraft.db` (favorites, collections, saved searches) —
 *   `clearAllTables()` keeps the schema so no migration/rebuild is needed.
 * - DataStore files: `wallkraft_settings`, `rotation`, `rotation_crops`,
 *   `search_history`. (Saved searches live in Room, not DataStore.)
 * - Encrypted prefs `wallkraft_secure_prefs` + plaintext-fallback
 *   `wallkraft_fallback_prefs`.
 * - Files: `filesDir/favorites` (offline favorite images); `cacheDir`
 *   subdirs `search_cache`, `coil`, `image_cache`, `crash`, `shared`,
 *   `share_cache`, `update`.
 * - Coil memory + disk caches via [ImageCache] (injected singleton and the
 *   static fallback instance, which back different loaders pre/post-Hilt).
 *
 * Every step is best-effort (`runCatching`) except the database clear: a
 * partial wipe must still continue through the remaining areas rather than
 * abort halfway and leave the app in a mixed state.
 */
@Singleton
class AndroidWipeBackend @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: WallKraftDatabase,
    private val imageCache: ImageCache,
) : WipeBackend {

    override suspend fun cancelRotationWork() {
        runCatching { RotationScheduler.cancelAll(context) }
    }

    override suspend fun clearDatabase() = withContext(Dispatchers.IO) {
        database.clearAllTables()
    }

    override suspend fun clearSettings() {
        runCatching { SettingsStore(context).clear() }
    }

    override suspend fun clearRotationSettings() {
        runCatching { RotationStore(context).clear() }
    }

    override suspend fun clearRotationCrops() {
        runCatching { RotationCropStore(context).clear() }
    }

    override suspend fun clearSearchHistory() {
        runCatching { SearchHistoryStore(context).clear() }
    }

    override suspend fun clearSecurePrefs() {
        runCatching { EncryptedApiKeyStore(context).clearApiKey() }
        runCatching { context.deleteSharedPreferences(SECURE_PREFS) }
        runCatching { context.deleteSharedPreferences(FALLBACK_PREFS) }
    }

    override suspend fun wipeFiles() = withContext(Dispatchers.IO) {
        runCatching { File(context.filesDir, "favorites").deleteRecursively() }
        CACHE_SUBDIRS.forEach { subdir ->
            runCatching { File(context.cacheDir, subdir).deleteRecursively() }
        }
    }

    override suspend fun evictImageCaches() {
        withContext(Dispatchers.IO) {
            runCatching { imageCache.memoryCache().clear() }
            runCatching { imageCache.diskCache().clear() }
            // Static fallback backs the pre-Hilt loaders (singleton + grid) —
            // same disk dir, separate memory instance. Clear both.
            runCatching { ImageCache.memoryCache(context).clear() }
            runCatching { ImageCache.diskCache(context).clear() }
        }
    }

    override suspend fun resetRotation() {
        runCatching { RotationScheduler.apply(context, RotationSchedule.OFF) }
    }

    companion object {
        private const val SECURE_PREFS = "wallkraft_secure_prefs"
        private const val FALLBACK_PREFS = "wallkraft_fallback_prefs"

        private val CACHE_SUBDIRS = listOf(
            "search_cache",
            "coil",
            "image_cache",
            "crash",
            "shared",
            "share_cache",
            "update",
        )
    }
}
