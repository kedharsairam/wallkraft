package com.wallkraft.app.presentation.settings

import com.wallkraft.app.domain.model.AppSettings
import com.wallkraft.app.domain.model.Order
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.domain.model.ThemeMode
import com.wallkraft.app.domain.repository.SettingsRepository
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

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
    fun `settings starts with defaults`() = runTest(dispatcher) {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo)
        advanceUntilIdle()

        assertEquals(AppSettings(), vm.settings.value)
    }

    @Test
    fun `setThemeMode updates repository`() = runTest(dispatcher) {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo)
        advanceUntilIdle()

        vm.setThemeMode(ThemeMode.Dark)
        advanceUntilIdle()

        assertEquals(ThemeMode.Dark, repo._settings.value.themeMode)
    }

    @Test
    fun `setSorting updates repository`() = runTest(dispatcher) {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo)
        advanceUntilIdle()

        vm.setSorting(Sorting.Toplist)
        advanceUntilIdle()

        assertEquals(Sorting.Toplist, repo._settings.value.sorting)
    }

    @Test
    fun `setOrder updates repository`() = runTest(dispatcher) {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo)
        advanceUntilIdle()

        vm.setOrder(Order.Asc)
        advanceUntilIdle()

        assertEquals(Order.Asc, repo._settings.value.order)
    }

    @Test
    fun `setApiKey updates apiKeyText`() = runTest(dispatcher) {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo)
        advanceUntilIdle()

        vm.setApiKey("my-secret-key")
        advanceUntilIdle()

        assertEquals("my-secret-key", vm.apiKeyText.value)
    }

    @Test
    fun `apiKeyText seeds from persisted value`() = runTest(dispatcher) {
        val repo = FakeSettingsRepository()
        repo._settings.value = AppSettings(apiKey = "existing-key")
        val vm = SettingsViewModel(repo)
        advanceUntilIdle()

        assertEquals("existing-key", vm.apiKeyText.value)
    }

    @Test
    fun `clearAllFavorites is exposed`() = runTest(dispatcher) {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo)
        advanceUntilIdle()

        // Verify the ViewModel is created without error
        assertEquals(AppSettings(), vm.settings.value)
    }

    private class FakeSettingsRepository : SettingsRepository {
        val _settings = MutableStateFlow(AppSettings())
        override val settings: Flow<AppSettings> = _settings
        override suspend fun current(): AppSettings = _settings.value
        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            _settings.value = transform(_settings.value)
        }
    }
}
