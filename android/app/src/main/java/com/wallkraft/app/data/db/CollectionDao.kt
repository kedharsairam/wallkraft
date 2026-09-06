package com.wallkraft.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Transaction
    @Query("SELECT * FROM collections ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CollectionWithItems>>

    /** Inserts, ignoring duplicates by name. Returns row id, or -1 if it exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCollection(collection: CollectionEntity): Long

    @Query("SELECT id FROM collections WHERE name = :name LIMIT 1")
    suspend fun findIdByName(name: String): Long?

    @Query("UPDATE collections SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    /** Items cascade-delete automatically. */
    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: Long)

    /** Adds a member; duplicate adds are ignored. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addItem(item: CollectionItemEntity)

    @Query("DELETE FROM collection_items WHERE collectionId = :collectionId AND wallpaperId = :wallpaperId")
    suspend fun removeItem(collectionId: Long, wallpaperId: String)

    @Query("SELECT collectionId FROM collection_items WHERE wallpaperId = :wallpaperId")
    fun observeIdsFor(wallpaperId: String): Flow<List<Long>>
}
