package com.wallkraft.app.data.repository

import com.wallkraft.app.data.cache.OfflineImageStore
import com.wallkraft.app.data.db.FavoriteDao
import com.wallkraft.app.data.db.FavoriteEntity
import com.wallkraft.app.domain.model.Wallpaper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Regression: unfavoriting must never touch the network.
 *
 * [FavoritesRepositoryImpl.remove] deletes the DB row plus the local file —
 * both local ops. The only network-touching op on [OfflineImageStore] is
 * [OfflineImageStore.save] (OkHttp download in FavoriteImageStore), so the
 * counting fake below fails the test if remove ever triggers a download.
 * Fakes follow the FavoritesViewModelTest pattern.
 */
class FavoritesRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `remove never touches network`() = runTest {
        val dao = FakeFavoriteDao()
        val store = CountingOfflineImageStore()
        val repo = FavoritesRepositoryImpl(dao, json, store)
        val wallpaper = Wallpaper(id = "wp-1", path = "https://example.com/wp-1.jpg")

        repo.add(wallpaper)
        store.localIds += "wp-1"
        assertEquals(0, store.saveCalls)

        repo.remove("wp-1")

        // save() performs the HTTP download — it must never run on unfavorite.
        assertEquals(0, store.saveCalls)
        // Local cleanup still happens: DB row gone, cached file deleted.
        assertFalse(repo.isFavorite("wp-1"))
        assertTrue("wp-1" in store.deletedIds)
    }

    @Test
    fun `remove unknown id never touches network`() = runTest {
        val dao = FakeFavoriteDao()
        val store = CountingOfflineImageStore()
        val repo = FavoritesRepositoryImpl(dao, json, store)

        repo.remove("wp-missing")

        assertEquals(0, store.saveCalls)
    }

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
    }

    private class CountingOfflineImageStore : OfflineImageStore {
        var saveCalls = 0
        val deletedIds = mutableListOf<String>()
        val localIds = mutableSetOf<String>()

        override fun fileFor(id: String): File? =
            if (id in localIds) File(id) else null

        override suspend fun save(wallpaper: Wallpaper): Boolean {
            saveCalls++
            localIds += wallpaper.id
            return true
        }

        override fun delete(id: String) {
            deletedIds += id
            localIds.remove(id)
        }
    }
}
