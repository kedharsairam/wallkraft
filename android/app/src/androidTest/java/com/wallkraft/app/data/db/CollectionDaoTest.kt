package com.wallkraft.app.data.db

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectionDaoTest {

    private lateinit var database: WallKraftDatabase
    private lateinit var dao: CollectionDao
    private lateinit var favorites: FavoriteDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, WallKraftDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.collectionDao()
        favorites = database.favoriteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun favorite(id: String) = FavoriteEntity(
        id = id,
        url = "https://wallhaven.cc/w/$id",
        path = "https://example.com/$id.jpg",
        thumbnail = "",
        thumbnailLarge = null,
        dimensionX = 1920,
        dimensionY = 1080,
        ratio = "16:9",
        fileSize = 1024L,
        favoritesCount = 0,
        category = "general",
        tagsJson = "[]",
        addedAt = 1000L,
    )

    @Test
    fun create_and_observe() = runTest {
        dao.insertCollection(CollectionEntity(name = "Beach", createdAt = 1000L))
        dao.insertCollection(CollectionEntity(name = "Dark", createdAt = 2000L))

        val all = dao.observeAll().first()
        assertEquals(listOf("Dark", "Beach"), all.map { it.collection.name })
        assertTrue(all.all { it.items.isEmpty() })
    }

    @Test
    fun duplicate_name_ignored() = runTest {
        val first = dao.insertCollection(CollectionEntity(name = "Beach", createdAt = 1000L))
        val second = dao.insertCollection(CollectionEntity(name = "Beach", createdAt = 2000L))

        assertTrue(first > 0)
        assertEquals(-1L, second)
        assertEquals(1, dao.observeAll().first().size)
        assertEquals(first, dao.findIdByName("Beach"))
    }

    @Test
    fun rename_updates_name() = runTest {
        val id = dao.insertCollection(CollectionEntity(name = "Beach", createdAt = 1000L))
        dao.rename(id, "Coast")

        assertEquals("Coast", dao.observeAll().first().single().collection.name)
    }

    @Test
    fun add_remove_membership() = runTest {
        favorites.upsert(favorite("w1"))
        val id = dao.insertCollection(CollectionEntity(name = "Beach", createdAt = 1000L))

        dao.addItem(CollectionItemEntity(id, "w1"))
        // Duplicate add is a no-op, not an error.
        dao.addItem(CollectionItemEntity(id, "w1"))
        assertEquals(listOf(id), dao.observeIdsFor("w1").first())
        assertEquals(listOf("w1"), dao.observeAll().first().single().items.map { it.wallpaperId })

        dao.removeItem(id, "w1")
        assertTrue(dao.observeIdsFor("w1").first().isEmpty())
    }

    @Test
    fun delete_collection_cascades_items() = runTest {
        favorites.upsert(favorite("w1"))
        val id = dao.insertCollection(CollectionEntity(name = "Beach", createdAt = 1000L))
        dao.addItem(CollectionItemEntity(id, "w1"))

        dao.deleteCollection(id)

        assertTrue(dao.observeAll().first().isEmpty())
        // Favorite itself survives.
        assertTrue(favorites.exists("w1"))
    }

    @Test
    fun delete_favorite_cascades_items() = runTest {
        favorites.upsert(favorite("w1"))
        val id = dao.insertCollection(CollectionEntity(name = "Beach", createdAt = 1000L))
        dao.addItem(CollectionItemEntity(id, "w1"))

        favorites.deleteById("w1")

        assertTrue(dao.observeAll().first().single().items.isEmpty())
    }
}
