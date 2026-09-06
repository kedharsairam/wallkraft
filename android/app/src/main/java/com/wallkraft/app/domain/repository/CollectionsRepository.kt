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
     * already exists. Returns -1 for blank names.
     */
    suspend fun create(name: String): Long
    suspend fun rename(id: Long, name: String)
    suspend fun delete(id: Long)
    suspend fun addTo(collectionId: Long, wallpaperId: String)
    suspend fun removeFrom(collectionId: Long, wallpaperId: String)
    fun observeIdsFor(wallpaperId: String): Flow<List<Long>>
}
