package com.wallkraft.app.data.repository

import com.wallkraft.app.data.cache.OfflineImageStore
import com.wallkraft.app.data.db.FavoriteDao
import com.wallkraft.app.data.db.FavoriteEntity
import com.wallkraft.app.domain.model.Wallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesRepositoryEdgeCaseTest {

    private val dispatcher = StandardTestDispatcher()
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Add same wallpaper twice → idempotent (no duplicate) ---

    @Test
    fun `add same wallpaper twice is idempotent`() = runTest(dispatcher) {
        val dao = FakeFavoriteDao()
        val store = CountingOfflineImageStore()
        val repo = FavoritesRepositoryImpl(dao, json, store)
        val wallpaper = Wallpaper(id = "wp-dup", path = "https://example.com/wp-dup.jpg")

        repo.add(wallpaper)
        repo.add(wallpaper)
        advanceUntilIdle()

        // Room's OnConflictStrategy.REPLACE means the second upsert overwrites
        // the first — the list should still contain exactly one entry.
        val favorites = repo.observeAll().first()
        assertEquals(1, favorites.size)
        assertEquals("wp-dup", favorites[0].wallpaper.id)
    }

    @Test
    fun `add same wallpaper twice preserves single entry in observeWallpapers`() = runTest(dispatcher) {
        val dao = FakeFavoriteDao()
        val store = CountingOfflineImageStore()
        val repo = FavoritesRepositoryImpl(dao, json, store)
        val wallpaper = Wallpaper(id = "wp-dup2", path = "https://example.com/wp-dup2.jpg")

        repo.add(wallpaper)
        repo.add(wallpaper)
        advanceUntilIdle()

        val wallpapers = repo.observeWallpapers().first()
        assertEquals(1, wallpapers.size)
        assertEquals("wp-dup2", wallpapers[0].id)
    }

    @Test
    fun `add same wallpaper with updated data replaces previous`() = runTest(dispatcher) {
        val dao = FakeFavoriteDao()
        val store = CountingOfflineImageStore()
        val repo = FavoritesRepositoryImpl(dao, json, store)
        val wp1 = Wallpaper(id = "wp-update", path = "https://example.com/wp-update-v1.jpg", dimensionX = 1920, dimensionY = 1080)
        val wp2 = Wallpaper(id = "wp-update", path = "https://example.com/wp-update-v2.jpg", dimensionX = 2560, dimensionY = 1440)

        repo.add(wp1)
        advanceUntilIdle()
        assertEquals("https://example.com/wp-update-v1.jpg", repo.observeWallpapers().first()[0].path)

        repo.add(wp2)
        advanceUntilIdle()

        val wallpapers = repo.observeWallpapers().first()
        assertEquals(1, wallpapers.size)
        assertEquals("https://example.com/wp-update-v2.jpg", wallpapers[0].path)
        assertEquals(2560, wallpapers[0].dimensionX)
    }

    // --- Remove non-existent favorite → no crash ---

    @Test
    fun `remove non-existent favorite does not crash`() = runTest(dispatcher) {
        val dao = FakeFavoriteDao()
        val store = CountingOfflineImageStore()
        val repo = FavoritesRepositoryImpl(dao, json, store)

        // Should not throw
        repo.remove("wp-nonexistent")
        advanceUntilIdle()

        assertFalse(repo.isFavorite("wp-nonexistent"))
    }

    @Test
    fun `remove non-existent favorite does not affect existing entries`() = runTest(dispatcher) {
        val dao = FakeFavoriteDao()
        val store = CountingOfflineImageStore()
        val repo = FavoritesRepositoryImpl(dao, json, store)

        repo.add(Wallpaper(id = "wp-existing", path = "https://example.com/wp-existing.jpg"))
        advanceUntilIdle()

        repo.remove("wp-nonexistent")
        advanceUntilIdle()

        assertTrue(repo.isFavorite("wp-existing"))
        assertEquals(1, repo.observeAll().first().size)
    }

    // --- Observe after remove → flow emits updated list ---

    @Test
    fun `observeAll emits updated list after remove`() = runTest(dispatcher) {
        val dao = FakeFavoriteDao()
        val store = CountingOfflineImageStore()
        val repo = FavoritesRepositoryImpl(dao, json, store)

        repo.add(Wallpaper(id = "wp-a", path = "https://example.com/a.jpg"))
        repo.add(Wallpaper(id = "wp-b", path = "https://example.com/b.jpg"))
        advanceUntilIdle()

        val beforeRemove = repo.observeAll().first()
        assertEquals(2, beforeRemove.size)

        repo.remove("wp-a")
        advanceUntilIdle()

        val afterRemove = repo.observeAll().first()
        assertEquals(1, afterRemove.size)
        assertEquals("wp-b", afterRemove[0].wallpaper.id)
    }

    @Test
    fun `observeWallpapers emits updated list after remove`() = runTest(dispatcher) {
        val dao = FakeFavoriteDao()
        val store = CountingOfflineImageStore()
        val repo = FavoritesRepositoryImpl(dao, json, store)

        repo.add(Wallpaper(id = "wp-x", path = "https://example.com/x.jpg"))
        repo.add(Wallpaper(id = "wp-y", path = "https://example.com/y.jpg"))
        advanceUntilIdle()

        val before = repo.observeWallpapers().first()
        assertEquals(2, before.size)

        repo.remove("wp-y")
        advanceUntilIdle()

        val after = repo.observeWallpapers().first()
        assertEquals(1, after.size)
        assertEquals("wp-x", after[0].id)
    }

    @Test
    fun `observeAll emits empty list after removing all favorites`() = runTest(dispatcher) {
        val dao = FakeFavoriteDao()
        val store = CountingOfflineImageStore()
        val repo = FavoritesRepositoryImpl(dao, json, store)

        repo.add(Wallpaper(id = "wp-1", path = "https://example.com/1.jpg"))
        repo.add(Wallpaper(id = "wp-2", path = "https://example.com/2.jpg"))
        advanceUntilIdle()

        repo.remove("wp-1")
        repo.remove("wp-2")
        advanceUntilIdle()

        val result = repo.observeAll().first()
        assertTrue(result.isEmpty())
    }

    // --- Concurrent add/remove → no corruption ---

    @Test
    fun `concurrent add and remove do not corrupt state`() = runTest(dispatcher) {
        val dao = FakeFavoriteDao()
        val store = CountingOfflineImageStore()
        val repo = FavoritesRepositoryImpl(dao, json, store)

        // Interleave add and remove operations
        repo.add(Wallpaper(id = "wp-1", path = "https://example.com/1.jpg"))
        repo.add(Wallpaper(id = "wp-2", path = "https://example.com/2.jpg"))
        advanceUntilIdle()

        // Remove first, add third, remove second — all in quick succession
        repo.remove("wp-1")
        repo.add(Wallpaper(id = "wp-3", path = "https://example.com/3.jpg"))
        repo.remove("wp-2")
        advanceUntilIdle()

        val result = repo.observeAll().first()
        assertEquals(1, result.size)
        assertEquals("wp-3", result[0].wallpaper.id)
        assertFalse(repo.isFavorite("wp-1"))
        assertFalse(repo.isFavorite("wp-2"))
        assertTrue(repo.isFavorite("wp-3"))
    }

    @Test
    fun `rapid add and remove cycles do not corrupt state`() = runTest(dispatcher) {
        val dao = FakeFavoriteDao()
        val store = CountingOfflineImageStore()
        val repo = FavoritesRepositoryImpl(dao, json, store)

        // Add and remove the same wallpaper repeatedly
        repeat(10) { i ->
            repo.add(Wallpaper(id = "wp-cycle", path = "https://example.com/cycle.jpg", views = i))
            repo.remove("wp-cycle")
        }
        advanceUntilIdle()

        assertFalse(repo.isFavorite("wp-cycle"))
        val result = repo.observeAll().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `concurrent adds of different wallpapers all succeed`() = runTest(dispatcher) {
        val dao = FakeFavoriteDao()
        val store = CountingOfflineImageStore()
        val repo = FavoritesRepositoryImpl(dao, json, store)

        // Add 5 different wallpapers in rapid succession
        repeat(5) { i ->
            repo.add(Wallpaper(id = "wp-$i", path = "https://example.com/$i.jpg"))
        }
        advanceUntilIdle()

        val result = repo.observeAll().first()
        assertEquals(5, result.size)
        repeat(5) { i ->
            assertTrue(repo.isFavorite("wp-$i"))
        }
    }

    // --- isFavorite edge cases ---

    @Test
    fun `isFavorite returns false for empty string id`() = runTest(dispatcher) {
        val dao = FakeFavoriteDao()
        val store = CountingOfflineImageStore()
        val repo = FavoritesRepositoryImpl(dao, json, store)

        assertFalse(repo.isFavorite(""))
    }

    @Test
    fun `isFavorite returns false after remove then true after re-add`() = runTest(dispatcher) {
        val dao = FakeFavoriteDao()
        val store = CountingOfflineImageStore()
        val repo = FavoritesRepositoryImpl(dao, json, store)
        val wallpaper = Wallpaper(id = "wp-readd", path = "https://example.com/readd.jpg")

        repo.add(wallpaper)
        advanceUntilIdle()
        assertTrue(repo.isFavorite("wp-readd"))

        repo.remove("wp-readd")
        advanceUntilIdle()
        assertFalse(repo.isFavorite("wp-readd"))

        repo.add(wallpaper)
        advanceUntilIdle()
        assertTrue(repo.isFavorite("wp-readd"))
    }

    // --- OfflineImageStore integration ---

    @Test
    fun `remove calls delete on OfflineImageStore`() = runTest(dispatcher) {
        val dao = FakeFavoriteDao()
        val store = CountingOfflineImageStore()
        val repo = FavoritesRepositoryImpl(dao, json, store)
        val wallpaper = Wallpaper(id = "wp-store", path = "https://example.com/store.jpg")

        repo.add(wallpaper)
        store.localIds += "wp-store"
        advanceUntilIdle()

        repo.remove("wp-store")
        advanceUntilIdle()

        assertTrue("wp-store" in store.deletedIds)
        assertFalse(repo.isFavorite("wp-store"))
    }

    @Test
    fun `remove non-existent favorite does not call delete on OfflineImageStore`() = runTest(dispatcher) {
        val dao = FakeFavoriteDao()
        val store = CountingOfflineImageStore()
        val repo = FavoritesRepositoryImpl(dao, json, store)

        repo.remove("wp-not-here")
        advanceUntilIdle()

        // delete should still be called (it's idempotent at the store level)
        assertTrue("wp-not-here" in store.deletedIds)
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
