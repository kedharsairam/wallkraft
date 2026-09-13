package com.wallkraft.app.presentation.favorites

import com.wallkraft.app.data.cache.OfflineImageStore
import com.wallkraft.app.data.prefs.RotationSettings
import com.wallkraft.app.data.prefs.RotationSettingsStore
import com.wallkraft.app.domain.model.AppSettings
import com.wallkraft.app.domain.model.RotationMode
import com.wallkraft.app.domain.model.RotationSchedule
import com.wallkraft.app.domain.model.RotationTarget
import com.wallkraft.app.domain.model.CropRect
import com.wallkraft.app.domain.model.Favorite
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.repository.CollectionsRepository
import com.wallkraft.app.domain.repository.FavoritesRepository
import com.wallkraft.app.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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
import java.io.File

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
        val vm = FavoritesViewModel(favRepo, FakeSettingsRepository(), FakeRotationStore(), FakeCollectionsRepository(), FakeOfflineImageStore())
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

        val vm = FavoritesViewModel(favRepo, FakeSettingsRepository(), FakeRotationStore(), FakeCollectionsRepository(), FakeOfflineImageStore())
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
        val vm = FavoritesViewModel(favRepo, FakeSettingsRepository(), FakeRotationStore(), FakeCollectionsRepository(), FakeOfflineImageStore())
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
        val vm = FavoritesViewModel(favRepo, FakeSettingsRepository(), FakeRotationStore(), FakeCollectionsRepository(), FakeOfflineImageStore())
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

        val vm = FavoritesViewModel(favRepo, FakeSettingsRepository(), FakeRotationStore(), FakeCollectionsRepository(), FakeOfflineImageStore())
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
        override fun observeWallpapers(): Flow<List<Wallpaper>> = _favorites.map { favs -> favs.map { it.wallpaper } }
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

    private class FakeSettingsRepository : SettingsRepository {
        private val _settings = MutableStateFlow(AppSettings())
        override val settings: Flow<AppSettings> = _settings
        override suspend fun current(): AppSettings = _settings.value
        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            _settings.value = transform(_settings.value)
        }
    }

    private class FakeRotationStore : RotationSettingsStore {
        private val _settings = MutableStateFlow(RotationSettings())
        override val settings = _settings
        override val timingWelcomeSeen = MutableStateFlow(true)
        override suspend fun current() = _settings.value
        override suspend fun setSchedule(schedule: RotationSchedule) { _settings.value = _settings.value.copy(schedule = schedule) }
        override suspend fun setMode(mode: RotationMode) { _settings.value = _settings.value.copy(mode = mode) }
        override suspend fun setTarget(target: RotationTarget) { _settings.value = _settings.value.copy(target = target) }
        override suspend fun setSourceCollection(id: Long?) { _settings.value = _settings.value.copy(sourceCollectionId = id) }
        override suspend fun setLastIndex(index: Int) { _settings.value = _settings.value.copy(lastIndex = index) }
        override suspend fun markTimingWelcomeSeen() {}
    }

    private class FakeCollectionsRepository : CollectionsRepository {
        override fun observeAll() = MutableStateFlow(emptyList<com.wallkraft.app.domain.model.Collection>())
        override fun observeIdsFor(wallpaperId: String) = MutableStateFlow(emptyList<Long>())
        override suspend fun create(name: String) = 0L
        override suspend fun rename(id: Long, name: String) = true
        override suspend fun delete(id: Long) {}
        override suspend fun addTo(collectionId: Long, wallpaperId: String) {}
        override suspend fun removeFrom(collectionId: Long, wallpaperId: String) {}
    }

    private class FakeOfflineImageStore : OfflineImageStore {
        override fun fileFor(id: String): File? = null
        override suspend fun save(wallpaper: Wallpaper): Boolean = false
        override fun delete(id: String) {}
    }
}
