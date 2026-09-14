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
class SavedSearchDaoTest {

    private lateinit var database: WallKraftDatabase
    private lateinit var dao: SavedSearchDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, WallKraftDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.savedSearchDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun search(
        name: String,
        lastUsedAt: Long = 1000L,
        query: String = "",
    ) = SavedSearchEntity(
        name = name,
        query = query,
        categories = "111",
        purity = "100",
        sorting = "date_added",
        topRange = "1M",
        colors = "",
        orientation = "both",
        createdAt = lastUsedAt,
        lastUsedAt = lastUsedAt,
    )

    @Test
    fun insert_and_observe_ordered_by_recency() = runTest {
        dao.insert(search("Old", lastUsedAt = 1000L))
        dao.insert(search("New", lastUsedAt = 2000L))

        val all = dao.observeAll().first()
        assertEquals(listOf("New", "Old"), all.map { it.name })
    }

    @Test
    fun duplicate_name_ignored() = runTest {
        val first = dao.insert(search("Blue"))
        val second = dao.insert(search("Blue"))

        assertTrue(first > 0)
        assertEquals(-1L, second)
        assertEquals(1, dao.observeAll().first().size)
        assertEquals(first, dao.findIdByName("Blue"))
    }

    @Test
    fun findIdByName_is_case_insensitive() = runTest {
        val id = dao.insert(search("Blue"))
        assertEquals(id, dao.findIdByName("BLUE"))
        assertEquals(id, dao.findIdByName("blue"))
        assertEquals(null, dao.findIdByName("Red"))
    }

    @Test
    fun touch_bumps_recency_count_and_reorders() = runTest {
        val old = dao.insert(search("Old", lastUsedAt = 1000L))
        dao.insert(search("New", lastUsedAt = 2000L))

        dao.touch(old, 3000L)

        val all = dao.observeAll().first()
        assertEquals(listOf("Old", "New"), all.map { it.name })
        assertEquals(3000L, all.first().lastUsedAt)
        assertEquals(1, all.first().useCount)
    }

    @Test
    fun rename_updates_name() = runTest {
        val id = dao.insert(search("Blue"))
        dao.rename(id, "Coast")

        assertEquals("Coast", dao.observeAll().first().single().name)
    }

    @Test
    fun deleteById_removes_row() = runTest {
        val id = dao.insert(search("Blue"))
        dao.deleteById(id)

        assertTrue(dao.observeAll().first().isEmpty())
        assertEquals(null, dao.findIdByName("Blue"))
    }
}
