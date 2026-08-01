package com.wallkraft.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class WallKraftDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
}
