package com.wallkraft.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedSearchDao {
    @Query("SELECT * FROM saved_searches ORDER BY lastUsedAt DESC, id DESC")
    fun observeAll(): Flow<List<SavedSearchEntity>>

    /** Case-insensitive lookup, so "Blue" and "blue" resolve to one row. */
    @Query("SELECT id FROM saved_searches WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findIdByName(name: String): Long?

    /** Inserts, ignoring duplicates by name. Returns row id, or -1 if it exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(search: SavedSearchEntity): Long

    /** Overwrites the filters of an existing row (re-save under the same name). */
    @Query(
        "UPDATE saved_searches SET query = :query, categories = :categories, " +
            "purity = :purity, sorting = :sorting, topRange = :topRange, " +
            "colors = :colors, orientation = :orientation, " +
            "lastUsedAt = :now, useCount = useCount + 1 WHERE id = :id",
    )
    suspend fun updateFilters(
        id: Long,
        query: String,
        categories: String,
        purity: String,
        sorting: String,
        topRange: String,
        colors: String,
        orientation: String,
        now: Long,
    )

    @Query("UPDATE saved_searches SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM saved_searches WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Bumps recency + usage (Browse apply, rotation pick). */
    @Query("UPDATE saved_searches SET lastUsedAt = :now, useCount = useCount + 1 WHERE id = :id")
    suspend fun touch(id: Long, now: Long)
}
