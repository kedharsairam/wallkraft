package com.wallkraft.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftTheme
import com.wallkraft.app.data.api.GithubApi
import com.wallkraft.app.data.api.RateLimitState
import com.wallkraft.app.data.api.WallhavenApi
import com.wallkraft.app.domain.model.AppSettings
import com.wallkraft.app.domain.repository.SettingsRepository
import com.wallkraft.app.domain.usecase.WipeAllDataUseCase
import com.wallkraft.app.domain.usecase.WipeBackend
import com.wallkraft.app.presentation.settings.DangerZoneViewModel
import com.wallkraft.app.presentation.settings.SettingsScreen
import com.wallkraft.app.presentation.settings.SettingsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for SettingsScreen — no Hilt, ViewModel constructed manually
 * with real API clients (no network calls made during composition).
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    private class FakeSettingsRepository : SettingsRepository {
        private val _settings = MutableStateFlow(AppSettings())
        override val settings: Flow<AppSettings> = _settings
        override suspend fun current(): AppSettings = _settings.value
        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            _settings.value = transform(_settings.value)
        }
    }

    private class FakeWipeBackend : WipeBackend {
        override suspend fun cancelRotationWork() {}
        override suspend fun clearDatabase() {}
        override suspend fun clearSettings() {}
        override suspend fun clearRotationSettings() {}
        override suspend fun clearRotationCrops() {}
        override suspend fun clearSearchHistory() {}
        override suspend fun clearSecurePrefs() {}
        override suspend fun wipeFiles() {}
        override suspend fun evictImageCaches() {}
        override suspend fun resetRotation() {}
    }

    private fun testContent() {
        val repo = FakeSettingsRepository()
        compose.setContent {
            KraftTheme {
                SettingsScreen(
                    viewModel = SettingsViewModel(
                        repo,
                        WallhavenApi(OkHttpClient(), kotlinx.serialization.json.Json {}, repo, RateLimitState()),
                        GithubApi(OkHttpClient(), kotlinx.serialization.json.Json {}),
                        { "Error" },
                    ),
                    dangerZoneViewModel = DangerZoneViewModel(WipeAllDataUseCase(FakeWipeBackend())),
                )
            }
        }
        compose.waitForIdle()
    }

    private fun resString(id: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    @Test
    fun browsing_section_is_displayed() {
        testContent()
        compose.onNodeWithText(resString(R.string.browsing_title)).assertIsDisplayed()
    }

    @Test
    fun data_section_is_displayed() {
        testContent()
        compose.onNodeWithText(resString(R.string.data_title)).assertIsDisplayed()
    }

    @Test
    fun about_section_is_displayed() {
        testContent()
        compose.onNodeWithText(resString(R.string.about_title))
            .performScrollTo()
            .assertIsDisplayed()
    }
}
