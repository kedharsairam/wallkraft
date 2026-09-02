package com.wallkraft.app.presentation.favorites

import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.model.Favorite
import com.wallkraft.app.domain.repository.FavoritesRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

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
    fun `favorites starts empty`() = runTest(dispatcher) {
        val favRepo = FakeFavoritesRepository()
        val vm = FavoritesViewModel(favRepo)
        // Launch a collector to activate WhileSubscribed
        val job = backgroundScope.launch { vm.favorites.collect {} }
        advanceUntilIdle()

        assertTrue(vm.favorites.value.isEmpty())
        job.cancel()
    }

    @Test
    fun `remove deletes from repository`() = runTest(dispatcher) {
        val favRepo = FakeFavoritesRepository()
        favRepo.addedIds.addAll(setOf("wp-1", "wp-2"))
        favRepo.emitFavorites()

        val vm = FavoritesViewModel(favRepo)
        val job = backgroundScope.launch { vm.favorites.collect {} }
        advanceUntilIdle()

        vm.remove("wp-1")
        advanceUntilIdle()

        assertFalse(favRepo.isFavorite("wp-1"))
        assertTrue(favRepo.isFavorite("wp-2"))
        job.cancel()
    }

    @Test
    fun `favorites updates when repository emits`() = runTest(dispatcher) {
        val favRepo = FakeFavoritesRepository()
        val vm = FavoritesViewModel(favRepo)
        val job = backgroundScope.launch { vm.favorites.collect {} }
        advanceUntilIdle()

        assertTrue(vm.favorites.value.isEmpty())

        favRepo.addedIds.addAll(setOf("wp-1", "wp-2"))
        favRepo.emitFavorites()
        advanceUntilIdle()

        assertEquals(2, vm.favorites.value.size)
        job.cancel()
    }

    @Test
    fun `remove non-existent favorite does not crash`() = runTest(dispatcher) {
        val favRepo = FakeFavoritesRepository()
        val vm = FavoritesViewModel(favRepo)
        val job = backgroundScope.launch { vm.favorites.collect {} }
        advanceUntilIdle()

        vm.remove("wp-999")
        advanceUntilIdle()

        assertTrue(vm.favorites.value.isEmpty())
        job.cancel()
    }

    @Test
    fun `favorites list is ordered by addedAt desc`() = runTest(dispatcher) {
        val favRepo = FakeFavoritesRepository()
        favRepo.addedWithTime["wp-1"] = 1000L
        favRepo.addedWithTime["wp-2"] = 3000L
        favRepo.addedWithTime["wp-3"] = 2000L
        favRepo.addedIds.addAll(setOf("wp-1", "wp-2", "wp-3"))
        favRepo.emitFavorites()

        val vm = FavoritesViewModel(favRepo)
        val job = backgroundScope.launch { vm.favorites.collect {} }
        advanceUntilIdle()

        assertEquals(listOf("wp-2", "wp-3", "wp-1"), vm.favorites.value.map { it.wallpaper.id })
        job.cancel()
    }

    private class FakeFavoritesRepository : FavoritesRepository {
        val addedIds = mutableSetOf<String>()
        val addedWithTime = mutableMapOf<String, Long>()
        private val _favorites = MutableStateFlow<List<Favorite>>(emptyList())

        fun emitFavorites() {
            _favorites.value = addedIds.map { id ->
                Favorite(
                    Wallpaper(id = id, dimensionX = 1920, dimensionY = 1080),
                    addedWithTime[id] ?: System.currentTimeMillis(),
                )
            }.sortedByDescending { it.addedAt }
        }

        override fun observeAll(): Flow<List<Favorite>> = _favorites
        override suspend fun isFavorite(id: String): Boolean = id in addedIds
        override suspend fun add(wallpaper: Wallpaper) {
            addedIds.add(wallpaper.id)
            emitFavorites()
        }
        override suspend fun remove(id: String) {
            addedIds.remove(id)
            emitFavorites()
        }
    }
}
