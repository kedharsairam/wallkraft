package com.wallkraft.app.domain.usecase

import com.wallkraft.app.domain.model.Collection
import com.wallkraft.app.domain.model.Favorite
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.repository.CollectionsRepository
import com.wallkraft.app.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FavoriteWallpaperUseCase encapsulates toggle/add/remove favorite and
 * add/remove from collection operations.
 */
class FavoriteWallpaperUseCaseTest {

    private class FakeFavoritesRepository : FavoritesRepository {
        val addedIds = mutableSetOf<String>()
        private val _favorites = MutableStateFlow<List<Favorite>>(emptyList())

        fun emitFavorites() {
            _favorites.value = addedIds.map { id ->
                Favorite(Wallpaper(id = id), System.currentTimeMillis())
            }
        }

        override fun observeAll(): Flow<List<Favorite>> = _favorites
        override fun observeWallpapers(): Flow<List<Wallpaper>> =
            _favorites.map { favs -> favs.map { it.wallpaper } }
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

    private class FakeCollectionsRepository : CollectionsRepository {
        val added = mutableListOf<Pair<Long, String>>()
        val removed = mutableListOf<Pair<Long, String>>()
        private val _collections = MutableStateFlow<List<Collection>>(emptyList())

        override fun observeAll(): Flow<List<Collection>> = _collections
        override fun observeIdsFor(wallpaperId: String): Flow<List<Long>> = MutableStateFlow(emptyList())
        override suspend fun create(name: String): Long = 1L
        override suspend fun rename(id: Long, name: String): Boolean = true
        override suspend fun delete(id: Long) {}
        override suspend fun addTo(collectionId: Long, wallpaperId: String) {
            added += collectionId to wallpaperId
        }
        override suspend fun removeFrom(collectionId: Long, wallpaperId: String) {
            removed += collectionId to wallpaperId
        }
    }

    @Test
    fun `toggleFavorite adds when not favorite`() = runTest {
        val favRepo = FakeFavoritesRepository()
        val colRepo = FakeCollectionsRepository()
        val useCase = FavoriteWallpaperUseCase(favRepo, colRepo)
        val wallpaper = Wallpaper(id = "wp-1")

        val result = useCase.toggleFavorite(wallpaper)

        assertTrue(result)
        assertTrue(favRepo.isFavorite("wp-1"))
    }

    @Test
    fun `toggleFavorite removes when already favorite`() = runTest {
        val favRepo = FakeFavoritesRepository()
        favRepo.addedIds.add("wp-1")
        favRepo.emitFavorites()
        val colRepo = FakeCollectionsRepository()
        val useCase = FavoriteWallpaperUseCase(favRepo, colRepo)
        val wallpaper = Wallpaper(id = "wp-1")

        val result = useCase.toggleFavorite(wallpaper)

        assertFalse(result)
        assertFalse(favRepo.isFavorite("wp-1"))
    }

    @Test
    fun `toggleFavorite adds when not favorite (non-existent)`() = runTest {
        val favRepo = FakeFavoritesRepository()
        val colRepo = FakeCollectionsRepository()
        val useCase = FavoriteWallpaperUseCase(favRepo, colRepo)
        val wallpaper = Wallpaper(id = "wp-999")

        val result = useCase.toggleFavorite(wallpaper)

        assertTrue(result)
        assertTrue(favRepo.isFavorite("wp-999"))
    }

    @Test
    fun `addToCollection delegates to repository`() = runTest {
        val favRepo = FakeFavoritesRepository()
        val colRepo = FakeCollectionsRepository()
        val useCase = FavoriteWallpaperUseCase(favRepo, colRepo)

        useCase.addToCollection(collectionId = 42L, wallpaperId = "wp-1")

        assertEquals(1, colRepo.added.size)
        assertEquals(42L to "wp-1", colRepo.added[0])
    }

    @Test
    fun `removeFromCollection delegates to repository`() = runTest {
        val favRepo = FakeFavoritesRepository()
        val colRepo = FakeCollectionsRepository()
        val useCase = FavoriteWallpaperUseCase(favRepo, colRepo)

        useCase.removeFromCollection(collectionId = 42L, wallpaperId = "wp-1")

        assertEquals(1, colRepo.removed.size)
        assertEquals(42L to "wp-1", colRepo.removed[0])
    }

    @Test
    fun `toggleFavorite works for multiple wallpapers independently`() = runTest {
        val favRepo = FakeFavoritesRepository()
        val colRepo = FakeCollectionsRepository()
        val useCase = FavoriteWallpaperUseCase(favRepo, colRepo)

        val wp1 = Wallpaper(id = "wp-1")
        val wp2 = Wallpaper(id = "wp-2")

        useCase.toggleFavorite(wp1)
        useCase.toggleFavorite(wp2)
        assertTrue(favRepo.isFavorite("wp-1"))
        assertTrue(favRepo.isFavorite("wp-2"))

        useCase.toggleFavorite(wp1)
        assertFalse(favRepo.isFavorite("wp-1"))
        assertTrue(favRepo.isFavorite("wp-2"))
    }
}
