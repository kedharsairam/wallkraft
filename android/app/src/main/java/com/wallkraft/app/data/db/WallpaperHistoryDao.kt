/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Kedhar Sairam
 */
package com.wallkraft.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperHistoryDao {
    @Query("SELECT * FROM wallpaper_history ORDER BY set_at DESC LIMIT 100")
    fun observeAll(): Flow<List<WallpaperHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WallpaperHistoryEntity)

    @Query("DELETE FROM wallpaper_history WHERE set_at < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)

    @Query("DELETE FROM wallpaper_history WHERE wallpaper_id = :wallpaperId AND set_at = :setAt")
    suspend fun deleteById(wallpaperId: String, setAt: Long)

    @Query("SELECT COUNT(*) FROM wallpaper_history")
    suspend fun count(): Int
}
