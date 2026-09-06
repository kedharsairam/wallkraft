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
 * v3 → v4 migration: existing favorites survive, collections tables appear.
 */
@RunWith(AndroidJUnit4::class)
class Migration34Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WallKraftDatabase::class.java,
    )

    @Test
    fun migrate3To4_keepsFavorites_createsCollections() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(
                "INSERT INTO favorites (id, url, path, thumbnail, thumbnailLarge, " +
                    "dimensionX, dimensionY, ratio, fileSize, favoritesCount, " +
                    "category, tagsJson, addedAt) VALUES " +
                    "('w1', 'u', 'p', '', NULL, 1920, 1080, '16:9', 1, 0, " +
                    "'general', '[]', 1000)",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB, 4, true, WallKraftDatabase.MIGRATION_3_4,
        ).apply {
            // Favorite intact.
            query("SELECT id FROM favorites").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("w1", cursor.getString(0))
            }
            // New tables usable: collection + member round-trip.
            execSQL("INSERT INTO collections (name, createdAt) VALUES ('Beach', 2000)")
            execSQL("INSERT INTO collection_items (collectionId, wallpaperId) VALUES (1, 'w1')")
            query("SELECT COUNT(*) FROM collection_items").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
            close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-34-test"
    }
}
