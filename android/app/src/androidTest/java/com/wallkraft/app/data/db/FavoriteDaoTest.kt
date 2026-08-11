package com.wallkraft.app.data.db

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoriteDaoTest {

    private lateinit var database: WallKraftDatabase
    private lateinit var dao: FavoriteDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, WallKraftDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.favoriteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun makeEntity(
        id: String,
        addedAt: Long = System.currentTimeMillis(),
        path: String = "https://example.com/$id.jpg",
    ) = FavoriteEntity(
        id = id,
        url = "https://wallhaven.cc/w/$id",
        path = path,
        thumbnail = "",
        thumbnailLarge = null,
        dimensionX = 1920,
        dimensionY = 1080,
        ratio = "16:9",
        fileSize = 1024L * 1024,
        favoritesCount = 0,
        category = "general",
        tagsJson = "[]",
        addedAt = addedAt,
    )

    @Test
    fun upsert_and_observeAll() = runTest {
        dao.upsert(makeEntity("wp-1"))

        val favorites = dao.observeAll().first()
        assertEquals(1, favorites.size)
        assertEquals("wp-1", favorites[0].id)
    }

    @Test
    fun upsert_replaces_existing() = runTest {
        dao.upsert(makeEntity("wp-1", addedAt = 1000L, path = "old.jpg"))
        dao.upsert(makeEntity("wp-1", addedAt = 2000L, path = "new.jpg"))

        val favorites = dao.observeAll().first()
        assertEquals(1, favorites.size)
        assertEquals("new.jpg", favorites[0].path)
    }

    @Test
    fun exists_returns_true_when_present() = runTest {
        dao.upsert(makeEntity("wp-1"))
        assertTrue(dao.exists("wp-1"))
    }

    @Test
    fun exists_returns_false_when_absent() = runTest {
        assertFalse(dao.exists("wp-999"))
    }

    @Test
    fun deleteById_removes_favorite() = runTest {
        dao.upsert(makeEntity("wp-1"))
        assertTrue(dao.exists("wp-1"))

        dao.deleteById("wp-1")
        assertFalse(dao.exists("wp-1"))
    }

    @Test
    fun deleteById_is_noop_when_absent() = runTest {
        dao.deleteById("wp-999")
        assertFalse(dao.exists("wp-999"))
    }

    @Test
    fun observeAll_ordered_by_addedAt_desc() = runTest {
        dao.upsert(makeEntity("wp-1", addedAt = 1000L))
        dao.upsert(makeEntity("wp-2", addedAt = 3000L))
        dao.upsert(makeEntity("wp-3", addedAt = 2000L))

        val favorites = dao.observeAll().first()
        assertEquals(listOf("wp-2", "wp-3", "wp-1"), favorites.map { it.id })
    }
}
