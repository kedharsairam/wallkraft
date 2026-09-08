package com.wallkraft.app.domain.repository

import com.wallkraft.app.data.db.CollectionWithItems
import kotlinx.coroutines.flow.Flow

/**
 * User-created favorites collections ("Beach", "Dark", …).
 *
 * One wallpaper can belong to several collections. Names are unique —
 * creating a duplicate resolves to the existing collection.
 */
interface CollectionsRepository {
    fun observeAll(): Flow<List<CollectionWithItems>>

    /**
     * Creates a collection. Returns the id, or the existing id when [name]
     * already exists (matched case-insensitively). Returns -1 for blank names.
     */
    suspend fun create(name: String): Long
    /**
     * Renames a collection. Returns false when [name] is taken by ANOTHER
     * collection (case-insensitive) — the row is left untouched. Blank names
     * are ignored and report success. Never throws for duplicates.
     */
    suspend fun rename(id: Long, name: String): Boolean
    suspend fun delete(id: Long)
    /**
     * Adds a member; duplicate adds are ignored. Also silently ignored when
     * the collection is gone or the wallpaper isn't favorited (FK) — callers
     * re-sync from the flow, so a stale tap is a no-op, never a crash.
     */
    suspend fun addTo(collectionId: Long, wallpaperId: String)
    suspend fun removeFrom(collectionId: Long, wallpaperId: String)
    fun observeIdsFor(wallpaperId: String): Flow<List<Long>>
}
