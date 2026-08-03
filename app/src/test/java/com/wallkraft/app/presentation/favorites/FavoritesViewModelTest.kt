package com.wallkraft.app.presentation.favorites

import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.repository.Favorite
import com.wallkraft.app.domain.repository.FavoritesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
        val vm = FavoritesViewModel(FakeFavoritesRepository())
        advanceUntilIdle()

        assertTrue(vm.favorites.value.isEmpty())
    }

    @Test
    fun `remove deletes from repository`() = runTest(dispatcher) {
        val favRepo = FakeFavoritesRepository()
        favRepo.addedIds.addAll(setOf("wp-1", "wp-2"))
        favRepo.emitFavorites()

        val vm = FavoritesViewModel(favRepo)
        advanceUntilIdle()

        vm.remove("wp-1")
        advanceUntilIdle()

        assertFalse(favRepo.isFavorite("wp-1"))
        assertTrue(favRepo.isFavorite("wp-2"))
    }

    private class FakeFavoritesRepository : FavoritesRepository {
        val addedIds = mutableSetOf<String>()
        private val _favorites = MutableStateFlow<List<Favorite>>(emptyList())

        fun emitFavorites() {
            _favorites.value = addedIds.map {
                Favorite(Wallpaper(id = it, dimensionX = 1920, dimensionY = 1080), System.currentTimeMillis())
            }
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
