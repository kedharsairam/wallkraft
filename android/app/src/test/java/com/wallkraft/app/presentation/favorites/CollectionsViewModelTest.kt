package com.wallkraft.app.presentation.favorites

import com.wallkraft.app.data.db.CollectionEntity
import com.wallkraft.app.data.db.CollectionItemEntity
import com.wallkraft.app.data.db.CollectionWithItems
import com.wallkraft.app.domain.repository.CollectionsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `collections starts empty`() = runTest(dispatcher) {
        val vm = CollectionsViewModel(FakeCollectionsRepository())
        val job = backgroundScope.launch { vm.collections.collect {} }
        advanceUntilIdle()

        assertTrue(vm.collections.value.isEmpty())
        job.cancel()
    }

    @Test
    fun `create then delete round trip`() = runTest(dispatcher) {
        val repo = FakeCollectionsRepository()
        val vm = CollectionsViewModel(repo)
        val job = backgroundScope.launch { vm.collections.collect {} }
        advanceUntilIdle()

        var created = -1L
        vm.create("Beach") { created = it }
        advanceUntilIdle()

        assertTrue(created > 0)
        assertEquals(listOf("Beach"), vm.collections.value.map { it.collection.name })

        vm.delete(created)
        advanceUntilIdle()

        assertTrue(vm.collections.value.isEmpty())
        job.cancel()
    }

    @Test
    fun `setMember toggles membership`() = runTest(dispatcher) {
        val repo = FakeCollectionsRepository()
        val vm = CollectionsViewModel(repo)
        val job = backgroundScope.launch { vm.collections.collect {} }
        advanceUntilIdle()

        var id = -1L
        vm.create("Beach") { id = it }
        advanceUntilIdle()

        vm.setMember(id, "w1", true)
        advanceUntilIdle()
        assertEquals(listOf("w1"), vm.collections.value.single().items.map { it.wallpaperId })

        vm.setMember(id, "w1", false)
        advanceUntilIdle()
        assertTrue(vm.collections.value.single().items.isEmpty())
        job.cancel()
    }

    @Test
    fun `rename duplicate reports false and keeps old name`() = runTest(dispatcher) {
        val vm = CollectionsViewModel(FakeCollectionsRepository())
        val job = backgroundScope.launch { vm.collections.collect {} }
        advanceUntilIdle()

        var a = -1L
        var b = -1L
        vm.create("Beach") { a = it }
        vm.create("Dunes") { b = it }
        advanceUntilIdle()
        assertEquals(1L, a)
        assertEquals(2L, b)

        var ok = true
        vm.rename(b, "beach") { ok = it }
        advanceUntilIdle()

        assertEquals(false, ok)
        assertEquals(
            listOf("Beach", "Dunes"),
            vm.collections.value.map { it.collection.name }.sorted(),
        )
        job.cancel()
    }

    @Test
    fun `rename success reports true`() = runTest(dispatcher) {
        val vm = CollectionsViewModel(FakeCollectionsRepository())
        val job = backgroundScope.launch { vm.collections.collect {} }
        advanceUntilIdle()

        var id = -1L
        vm.create("Beach") { id = it }
        advanceUntilIdle()

        var ok = false
        vm.rename(id, "Coast") { ok = it }
        advanceUntilIdle()

        assertEquals(true, ok)
        assertEquals(listOf("Coast"), vm.collections.value.map { it.collection.name })
        job.cancel()
    }

    private class FakeCollectionsRepository : CollectionsRepository {
        private val collections = mutableMapOf<Long, String>()
        private val items = mutableSetOf<CollectionItemEntity>()
        private var nextId = 1L
        private val _all = MutableStateFlow<List<CollectionWithItems>>(emptyList())

        private fun emit() {
            _all.value = collections.map { (id, name) ->
                CollectionWithItems(
                    CollectionEntity(id, name, 1000L),
                    items.filter { it.collectionId == id },
                )
            }
        }

        override fun observeAll(): Flow<List<CollectionWithItems>> = _all

        override suspend fun create(name: String): Long {
            val cleaned = name.trim()
            if (cleaned.isEmpty()) return -1
            collections.entries.firstOrNull { it.value.equals(cleaned, ignoreCase = true) }?.let { return it.key }
            val id = nextId++
            collections[id] = cleaned
            emit()
            return id
        }

        override suspend fun rename(id: Long, name: String): Boolean {
            val cleaned = name.trim()
            if (cleaned.isEmpty()) return true
            if (collections.any { (otherId, otherName) -> otherId != id && otherName.equals(cleaned, ignoreCase = true) }) {
                return false
            }
            collections[id] = cleaned
            emit()
            return true
        }

        override suspend fun delete(id: Long) {
            collections.remove(id)
            items.removeAll { it.collectionId == id }
            emit()
        }

        override suspend fun addTo(collectionId: Long, wallpaperId: String) {
            items += CollectionItemEntity(collectionId, wallpaperId)
            emit()
        }

        override suspend fun removeFrom(collectionId: Long, wallpaperId: String) {
            items.removeIf { it.collectionId == collectionId && it.wallpaperId == wallpaperId }
            emit()
        }

        override fun observeIdsFor(wallpaperId: String): Flow<List<Long>> =
            MutableStateFlow(items.filter { it.wallpaperId == wallpaperId }.map { it.collectionId })
    }
}
