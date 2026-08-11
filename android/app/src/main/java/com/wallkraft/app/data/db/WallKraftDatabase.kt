package com.wallkraft.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FavoriteEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class WallKraftDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        /** v1 → v2: add the nullable `collection` column for favorites folders. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE favorites ADD COLUMN collection TEXT")
            }
        }

        /**
         * v2 → v3: drop the `collection` column (favorites folders removed).
         * Recreate the table instead of `DROP COLUMN`, which needs SQLite
         * 3.35+ (API 33+) — this works on every supported device.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE favorites_new (" +
                        "id TEXT NOT NULL, " +
                        "url TEXT NOT NULL, " +
                        "path TEXT NOT NULL, " +
                        "thumbnail TEXT NOT NULL, " +
                        "thumbnailLarge TEXT, " +
                        "dimensionX INTEGER NOT NULL, " +
                        "dimensionY INTEGER NOT NULL, " +
                        "ratio TEXT NOT NULL, " +
                        "fileSize INTEGER NOT NULL, " +
                        "favoritesCount INTEGER NOT NULL, " +
                        "category TEXT NOT NULL, " +
                        "tagsJson TEXT NOT NULL, " +
                        "addedAt INTEGER NOT NULL, " +
                        "PRIMARY KEY(id))",
                )
                db.execSQL(
                    "INSERT INTO favorites_new (id, url, path, thumbnail, thumbnailLarge, dimensionX, dimensionY, ratio, fileSize, favoritesCount, category, tagsJson, addedAt) " +
                        "SELECT id, url, path, thumbnail, thumbnailLarge, dimensionX, dimensionY, ratio, fileSize, favoritesCount, category, tagsJson, addedAt FROM favorites",
                )
                db.execSQL("DROP TABLE favorites")
                db.execSQL("ALTER TABLE favorites_new RENAME TO favorites")
            }
        }
    }
}
