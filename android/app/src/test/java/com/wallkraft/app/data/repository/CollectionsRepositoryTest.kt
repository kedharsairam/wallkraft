package com.wallkraft.app.data.repository

import com.wallkraft.app.data.db.CollectionDao
import com.wallkraft.app.data.db.CollectionEntity
import com.wallkraft.app.data.db.CollectionItemEntity
import com.wallkraft.app.data.db.CollectionWithItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionsRepositoryTest {

    private class FakeDao : CollectionDao {
        val collections = mutableMapOf<Long, CollectionEntity>()
        val items = mutableSetOf<CollectionItemEntity>()
        var nextId = 1L
        private val version = MutableStateFlow(0)

        private fun snapshot() = collections.values
            .sortedByDescending { it.createdAt }
            .map { c ->
                CollectionWithItems(c, items.filter { it.collectionId == c.id })
            }

        override fun observeAll(): Flow<List<CollectionWithItems>> =
            version.map { snapshot() }

        override suspend fun insertCollection(collection: CollectionEntity): Long {
            if (collections.values.any { it.name == collection.name }) return -1
            val id = nextId++
            collections[id] = collection.copy(id = id)
            version.update { it + 1 }
            return id
        }

        override suspend fun findIdByName(name: String): Long? =
            collections.entries.firstOrNull { it.value.name.equals(name, ignoreCase = true) }?.key

        override suspend fun rename(id: Long, name: String) {
            collections[id]?.let {
                collections[id] = it.copy(name = name)
                version.update { v -> v + 1 }
            }
        }

        override suspend fun deleteCollection(id: Long) {
            collections.remove(id)
            items.removeAll { it.collectionId == id }
            version.update { it + 1 }
        }

        override suspend fun addItem(item: CollectionItemEntity) {
            items += item
            version.update { it + 1 }
        }

        override suspend fun removeItem(collectionId: Long, wallpaperId: String) {
            items.removeIf { it.collectionId == collectionId && it.wallpaperId == wallpaperId }
            version.update { it + 1 }
        }

        override fun observeIdsFor(wallpaperId: String): Flow<List<Long>> =
            version.map { items.filter { it.wallpaperId == wallpaperId }.map { it.collectionId } }
    }

    @Test
    fun create_trims_and_rejects_blank() = runTest {
        val repo = CollectionsRepositoryImpl(FakeDao())
        assertEquals(1L, repo.create("  Beach  "))
        assertEquals(-1L, repo.create("   "))
    }

    @Test
    fun create_duplicate_resolves_existing() = runTest {
        val repo = CollectionsRepositoryImpl(FakeDao())
        val first = repo.create("Beach")
        assertEquals(first, repo.create("Beach"))
    }

    @Test
    fun rename_ignores_blank() = runTest {
        val dao = FakeDao()
        val repo = CollectionsRepositoryImpl(dao)
        val id = repo.create("Beach")
        assertTrue(repo.rename(id, "   "))
        assertEquals("Beach", dao.collections[id]!!.name)
        assertTrue(repo.rename(id, "Coast"))
        assertEquals("Coast", dao.collections[id]!!.name)
    }

    @Test
    fun rename_duplicate_returns_false_and_keeps_name() = runTest {
        val dao = FakeDao()
        val repo = CollectionsRepositoryImpl(dao)
        repo.create("Beach")
        val other = repo.create("Dunes")
        assertEquals(false, repo.rename(other, "BEACH"))
        assertEquals("Dunes", dao.collections[other]!!.name)
    }

    @Test
    fun create_case_variant_resolves_existing() = runTest {
        val repo = CollectionsRepositoryImpl(FakeDao())
        val first = repo.create("Beach")
        assertEquals(first, repo.create("beach"))
    }

    @Test
    fun membership_round_trip() = runTest {
        val dao = FakeDao()
        val repo = CollectionsRepositoryImpl(dao)
        val id = repo.create("Beach")

        repo.addTo(id, "w1")
        repo.addTo(id, "w1") // duplicate no-op
        assertEquals(listOf(id), dao.observeIdsFor("w1").first())

        repo.removeFrom(id, "w1")
        assertTrue(dao.items.isEmpty())
    }

    @Test
    fun delete_removes_collection() = runTest {
        val dao = FakeDao()
        val repo = CollectionsRepositoryImpl(dao)
        val id = repo.create("Beach")
        repo.delete(id)
        assertTrue(dao.collections.isEmpty())
    }
}
