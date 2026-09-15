package com.wallkraft.app.data.repository

import com.wallkraft.app.data.cache.OfflineImageStore
import com.wallkraft.app.data.db.FavoriteDao
import com.wallkraft.app.data.db.FavoriteEntity
import com.wallkraft.app.domain.model.Wallpaper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Offline behavior for [FavoritesRepositoryImpl].
 *
 * Favorites are stored entirely in Room — no network required. These tests
 * verify add/remove/observe work without connectivity, and that repair
 * correctly skips network-dependent operations when offline.
 */
class FavoritesOfflineTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun wallpaper(id: String) = Wallpaper(
        id = id,
        path = "https://example.com/$id.jpg",
    )

    // --- Add favorite offline ---

    @Test
    fun `add favorite offline persists to Room`() = runTest {
        val dao = FakeFavoriteDao()
        val store = StubOfflineImageStore()
        val repo = FavoritesRepositoryImpl(dao, json, store)
        val wp = wallpaper("wp-offline-1")

        repo.add(wp)

        assertTrue(repo.isFavorite("wp-offline-1"))
        assertEquals(1, dao.observeAll().first().size)
        assertEquals("wp-offline-1", dao.observeAll().first()[0].id)
    }

    // --- Remove favorite offline ---

    @Test
    fun `remove favorite offline works`() = runTest {
        val dao = FakeFavoriteDao()
        val store = StubOfflineImageStore()
        val repo = FavoritesRepositoryImpl(dao, json, store)
        val wp = wallpaper("wp-offline-2")

        repo.add(wp)
        assertTrue(repo.isFavorite("wp-offline-2"))

        repo.remove("wp-offline-2")

        assertFalse(repo.isFavorite("wp-offline-2"))
        assertEquals(0, dao.observeAll().first().size)
    }

    // --- Observe favorites offline ---

    @Test
    fun `observe favorites offline returns local data`() = runTest {
        val dao = FakeFavoriteDao()
        val store = StubOfflineImageStore()
        val repo = FavoritesRepositoryImpl(dao, json, store)

        repo.add(wallpaper("wp1"))
        repo.add(wallpaper("wp2"))
        repo.add(wallpaper("wp3"))

        val favorites = repo.observeAll().first()

        assertEquals(3, favorites.size)
        val ids = favorites.map { it.wallpaper.id }
        assertTrue(ids.containsAll(listOf("wp1", "wp2", "wp3")))
    }

    // --- Repair offline skips network ---

    @Test
    fun `repair missing files offline skips download`() = runTest {
        val store = StubOfflineImageStore()
        val repair = com.wallkraft.app.data.cache.FavoriteOfflineRepair(store)

        val wallpapers = listOf(
            wallpaper("wp-a"),
            wallpaper("wp-b"),
        )

        // No files exist locally — repair would attempt downloads
        val missing = repair.missing(wallpapers)
        assertEquals(2, missing.size)

        // Calling repairAll with a failing store simulates offline —
        // it should not crash, just report failures.
        store.failSave = true
        val result = repair.repairAll(missing)

        assertEquals(0, result.restored)
        assertEquals(2, result.failed.size)
        assertTrue(result.failed.containsAll(listOf("wp-a", "wp-b")))
    }

    @Test
    fun `repair skips already-present local files`() = runTest {
        val store = StubOfflineImageStore()
        store.localIds += "wp-present"
        val repair = com.wallkraft.app.data.cache.FavoriteOfflineRepair(store)

        val wallpapers = listOf(wallpaper("wp-present"), wallpaper("wp-absent"))

        val missing = repair.missing(wallpapers)
        assertEquals(1, missing.size)
        assertEquals("wp-absent", missing[0].id)
    }

    // --- Fakes ---

    private class FakeFavoriteDao : FavoriteDao {
        private val rows = mutableMapOf<String, FavoriteEntity>()
        private val flow = MutableStateFlow<List<FavoriteEntity>>(emptyList())

        private fun emit() {
            flow.value = rows.values.sortedByDescending { it.addedAt }
        }

        override fun observeAll(): Flow<List<FavoriteEntity>> = flow
        override suspend fun exists(id: String): Boolean = id in rows
        override suspend fun upsert(favorite: FavoriteEntity) {
            rows[favorite.id] = favorite
            emit()
        }
        override suspend fun deleteById(id: String) {
            rows.remove(id)
            emit()
        }
        override fun getAllBlocking(): List<FavoriteEntity> = rows.values.toList()
        override fun getByIdBlocking(id: String): FavoriteEntity? = rows[id]
    }

    private class StubOfflineImageStore : OfflineImageStore {
        var failSave = false
        val localIds = mutableSetOf<String>()

        override fun fileFor(id: String): File? =
            if (id in localIds) File("/tmp/$id") else null

        override suspend fun save(wallpaper: Wallpaper): Boolean {
            if (failSave) return false
            localIds += wallpaper.id
            return true
        }

        override fun delete(id: String) {
            localIds.remove(id)
        }
    }
}
