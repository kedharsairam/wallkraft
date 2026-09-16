/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Kedhar Sairam
 */
package com.wallkraft.app.domain.usecase

import javax.inject.Inject

/**
 * GDPR erasure backend — one suspend step per storage area.
 *
 * Extracted as an interface (same precedent as `OfflineImageStore` for
 * `FavoriteOfflineRepair`) so [WipeAllDataUseCase] ordering is unit-testable
 * against a fake on the JVM, where Room/DataStore/WorkManager can't run.
 * The production implementation is `AndroidWipeBackend` in `data/wipe`.
 */
interface WipeBackend {
    /** Cancel the WorkManager rotation chain (first, so workers go quiet). */
    suspend fun cancelRotationWork()

    /** Clear all Room tables, keeping the schema (`wallkraft.db`). */
    suspend fun clearDatabase()

    /** Clear the `wallkraft_settings` DataStore file. */
    suspend fun clearSettings()

    /** Clear the `rotation` DataStore file. */
    suspend fun clearRotationSettings()

    /** Clear the `rotation_crops` DataStore file. */
    suspend fun clearRotationCrops()

    /** Clear the `search_history` DataStore file. */
    suspend fun clearSearchHistory()

    /** Clear the encrypted API key + delete secure/fallback prefs files. */
    suspend fun clearSecurePrefs()

    /** Delete `filesDir/favorites` and disposable `cacheDir` subdirectories. */
    suspend fun wipeFiles()

    /** Evict Coil memory + disk caches. */
    suspend fun evictImageCaches()

    /** Re-apply rotation OFF so the scheduler is in a clean state. */
    suspend fun resetRotation()
}

/**
 * In-app "Delete all data" (GDPR erasure).
 *
 * Order matters: workers are silenced first, the database is emptied before
 * the preferences that describe it, files and image caches go next, and the
 * rotation scheduler is reset to OFF last so a clean state is re-seeded.
 */
class WipeAllDataUseCase @Inject constructor(
    private val backend: WipeBackend,
) {
    suspend fun wipeAll() {
        backend.cancelRotationWork()
        backend.clearDatabase()
        backend.clearSettings()
        backend.clearRotationSettings()
        backend.clearRotationCrops()
        backend.clearSearchHistory()
        backend.clearSecurePrefs()
        backend.wipeFiles()
        backend.evictImageCaches()
        backend.resetRotation()
    }
}
