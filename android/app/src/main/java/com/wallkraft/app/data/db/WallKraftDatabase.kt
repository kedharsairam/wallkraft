package com.wallkraft.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FavoriteEntity::class, CollectionEntity::class, CollectionItemEntity::class, SavedSearchEntity::class, WallpaperHistoryEntity::class],
    version = 6,
    exportSchema = true,
)
/** Room database for WallKraft — stores favorites, collections, saved searches, and wallpaper history. */
abstract class WallKraftDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun collectionDao(): CollectionDao
    abstract fun savedSearchDao(): SavedSearchDao
    abstract fun wallpaperHistoryDao(): WallpaperHistoryDao

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
        /**
         * v3 → v4: collections for organizing favorites (many-to-many via
         * collection_items, cascading on both sides). Fresh tables — no data
         * moves, existing favorites are untouched.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `collections` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_collections_name` " +
                        "ON `collections` (`name`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `collection_items` " +
                        "(`collectionId` INTEGER NOT NULL, `wallpaperId` TEXT NOT NULL, " +
                        "PRIMARY KEY(`collectionId`, `wallpaperId`), " +
                        "FOREIGN KEY(`collectionId`) REFERENCES `collections`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(`wallpaperId`) REFERENCES `favorites`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_collection_items_wallpaperId` " +
                        "ON `collection_items` (`wallpaperId`)",
                )
            }
        }
        /**
         * v4 → v5: saved searches (reusable Browse filter sets, ordered by
         * recency). Fresh table + indices — no data moves, existing favorites
         * and collections are untouched.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `saved_searches` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `query` TEXT NOT NULL, " +
                        "`categories` TEXT NOT NULL, `purity` TEXT NOT NULL, " +
                        "`sorting` TEXT NOT NULL, `topRange` TEXT NOT NULL, " +
                        "`colors` TEXT NOT NULL, `orientation` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, `lastUsedAt` INTEGER NOT NULL, " +
                        "`useCount` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_saved_searches_name` " +
                        "ON `saved_searches` (`name`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_saved_searches_lastUsedAt` " +
                        "ON `saved_searches` (`lastUsedAt`)",
                )
            }
        }
        /** v5 → v6: wallpaper history (recently set wallpapers with dates). */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `wallpaper_history` (" +
                        "`wallpaper_id` TEXT NOT NULL, " +
                        "`path` TEXT NOT NULL, " +
                        "`thumbnail` TEXT NOT NULL, " +
                        "`set_at` INTEGER NOT NULL, " +
                        "`source` TEXT NOT NULL, " +
                        "PRIMARY KEY(`wallpaper_id`, `set_at`))",
                )
            }
        }
    }
}
