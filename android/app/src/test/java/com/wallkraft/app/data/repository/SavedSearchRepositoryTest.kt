package com.wallkraft.app.data.repository

import com.wallkraft.app.data.db.SavedSearchDao
import com.wallkraft.app.data.db.SavedSearchEntity
import com.wallkraft.app.domain.model.Category
import com.wallkraft.app.domain.model.Purity
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.domain.model.WallhavenFilters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedSearchRepositoryTest {

    private class FakeDao : SavedSearchDao {
        val searches = mutableMapOf<Long, SavedSearchEntity>()
        var nextId = 1L
        private val version = MutableStateFlow(0)

        override fun observeAll(): Flow<List<SavedSearchEntity>> =
            version.map {
                searches.values.sortedWith(
                    compareByDescending<SavedSearchEntity> { it.lastUsedAt }.thenByDescending { it.id },
                )
            }

        override suspend fun findIdByName(name: String): Long? =
            searches.entries.firstOrNull { it.value.name.equals(name, ignoreCase = true) }?.key

        override suspend fun insert(search: SavedSearchEntity): Long {
            if (searches.values.any { it.name == search.name }) return -1
            val id = nextId++
            searches[id] = search.copy(id = id)
            version.update { it + 1 }
            return id
        }

        override suspend fun updateFilters(
            id: Long,
            query: String,
            categories: String,
            purity: String,
            sorting: String,
            topRange: String,
            colors: String,
            orientation: String,
            now: Long,
        ) {
            searches[id]?.let {
                searches[id] = it.copy(
                    query = query,
                    categories = categories,
                    purity = purity,
                    sorting = sorting,
                    topRange = topRange,
                    colors = colors,
                    orientation = orientation,
                    lastUsedAt = now,
                    useCount = it.useCount + 1,
                )
                version.update { v -> v + 1 }
            }
        }

        override suspend fun rename(id: Long, name: String) {
            searches[id]?.let {
                searches[id] = it.copy(name = name)
                version.update { v -> v + 1 }
            }
        }

        override suspend fun deleteById(id: Long) {
            searches.remove(id)
            version.update { it + 1 }
        }

        override suspend fun touch(id: Long, now: Long) {
            searches[id]?.let {
                searches[id] = it.copy(lastUsedAt = now, useCount = it.useCount + 1)
                version.update { v -> v + 1 }
            }
        }
    }

    @Test
    fun save_trims_and_rejects_blank() = runTest {
        val repo = SavedSearchRepositoryImpl(FakeDao())
        assertEquals(1L, repo.save("  Blue  ", WallhavenFilters()))
        assertEquals(-1L, repo.save("   ", WallhavenFilters()))
    }

    @Test
    fun save_duplicate_overwrites_and_resolves_existing() = runTest {
        val dao = FakeDao()
        val repo = SavedSearchRepositoryImpl(dao)
        val first = repo.save("Blue", WallhavenFilters(query = "blue"))
        val second = repo.save("BLUE", WallhavenFilters(query = "blue sky"))

        assertEquals(first, second)
        assertEquals("blue sky", dao.searches[first]!!.query)
    }

    @Test
    fun rename_ignores_blank() = runTest {
        val dao = FakeDao()
        val repo = SavedSearchRepositoryImpl(dao)
        val id = repo.save("Blue", WallhavenFilters())
        assertTrue(repo.rename(id, "   "))
        assertEquals("Blue", dao.searches[id]!!.name)
        assertTrue(repo.rename(id, "Coast"))
        assertEquals("Coast", dao.searches[id]!!.name)
    }

    @Test
    fun rename_duplicate_returns_false_and_keeps_name() = runTest {
        val dao = FakeDao()
        val repo = SavedSearchRepositoryImpl(dao)
        repo.save("Blue", WallhavenFilters())
        val other = repo.save("Dunes", WallhavenFilters())
        assertEquals(false, repo.rename(other, "BLUE"))
        assertEquals("Dunes", dao.searches[other]!!.name)
    }

    @Test
    fun recordUse_bumps_count() = runTest {
        val dao = FakeDao()
        val repo = SavedSearchRepositoryImpl(dao)
        val id = repo.save("Blue", WallhavenFilters())
        repo.recordUse(id)
        assertEquals(1, dao.searches[id]!!.useCount)
    }

    @Test
    fun observeAll_ordered_by_recency() = runTest {
        val dao = FakeDao()
        val repo = SavedSearchRepositoryImpl(dao)
        val filters = WallhavenFilters()
        val first = repo.save("First", filters.copy(query = "a"))
        val second = repo.save("Second", filters.copy(query = "b"))
        assertEquals(listOf(second, first), repo.observeAll().first().map { it.id })

        // Touching the older search moves it to the top.
        dao.touch(first, System.currentTimeMillis() + 60_000)
        assertEquals(listOf(first, second), repo.observeAll().first().map { it.id })
    }

    @Test
    fun save_persists_filter_columns() = runTest {
        val dao = FakeDao()
        val repo = SavedSearchRepositoryImpl(dao)
        val filters = WallhavenFilters(
            categories = setOf(Category.Anime),
            purity = setOf(Purity.SFW, Purity.Sketchy),
            sorting = Sorting.Toplist,
        )
        val id = repo.save("Anime top", filters)
        val saved = dao.searches[id]!!
        assertEquals("010", saved.categories)
        assertEquals("110", saved.purity)
        assertEquals("toplist", saved.sorting)
        assertEquals(filters, saved.toFilters())
    }

    @Test
    fun delete_removes_search() = runTest {
        val dao = FakeDao()
        val repo = SavedSearchRepositoryImpl(dao)
        val id = repo.save("Blue", WallhavenFilters())
        repo.delete(id)
        assertTrue(dao.searches.isEmpty())
    }
}
