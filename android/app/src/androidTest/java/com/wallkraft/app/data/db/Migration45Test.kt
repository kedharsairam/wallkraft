package com.wallkraft.app.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * v4 → v5 migration: existing favorites survive, saved_searches table appears.
 */
@RunWith(AndroidJUnit4::class)
class Migration45Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WallKraftDatabase::class.java,
    )

    @Test
    fun migrate4To5_keepsFavorites_createsSavedSearches() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL(
                "INSERT INTO favorites (id, url, path, thumbnail, thumbnailLarge, " +
                    "dimensionX, dimensionY, ratio, fileSize, favoritesCount, " +
                    "category, tagsJson, addedAt) VALUES " +
                    "('w1', 'u', 'p', '', NULL, 1920, 1080, '16:9', 1, 0, " +
                    "'general', '[]', 1000)",
            )
            execSQL("INSERT INTO collections (name, createdAt) VALUES ('Beach', 2000)")
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB, 5, true, WallKraftDatabase.MIGRATION_4_5,
        ).apply {
            // Favorites and collections intact.
            query("SELECT id FROM favorites").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("w1", cursor.getString(0))
            }
            query("SELECT name FROM collections").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Beach", cursor.getString(0))
            }
            // New table usable: saved search round-trip.
            execSQL(
                "INSERT INTO saved_searches (name, query, categories, purity, " +
                    "sorting, topRange, colors, orientation, createdAt, " +
                    "lastUsedAt, useCount) VALUES " +
                    "('Blue', 'blue sky', '111', '100', 'date_added', '1M', " +
                    "'', 'both', 3000, 3000, 0)",
            )
            query("SELECT query, useCount FROM saved_searches WHERE name = 'Blue'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("blue sky", cursor.getString(0))
                assertEquals(0, cursor.getInt(1))
            }
            close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-45-test"
    }
}
