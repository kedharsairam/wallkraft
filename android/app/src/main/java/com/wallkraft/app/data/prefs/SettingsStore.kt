package com.wallkraft.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wallkraft.app.domain.model.AppSettings
import com.wallkraft.app.domain.model.Category
import com.wallkraft.app.domain.model.Orientation
import com.wallkraft.app.domain.model.Purity
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.domain.model.ThemeMode
import com.wallkraft.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.wallKraftDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "wallkraft_settings",
)

class SettingsStore(private val context: Context) : SettingsRepository {

    private val encryptedKeyStore = EncryptedApiKeyStore(context)

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CATEGORIES = stringPreferencesKey("categories")
        val PURITY = stringPreferencesKey("purity")
        val SORTING = stringPreferencesKey("sorting")
        val ORIENTATION = stringPreferencesKey("orientation")
        val DATA_SAVER_MODE = booleanPreferencesKey("data_saver_mode")
    }

    override val settings: Flow<AppSettings> = context.wallKraftDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs.toSettings(encryptedKeyStore.getApiKey()) }

    override suspend fun current(): AppSettings = settings.first()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        // Read current API key from encrypted store inside the edit block for atomicity.
        context.wallKraftDataStore.edit { prefs ->
            val currentApiKey = encryptedKeyStore.getApiKey()
            val current = prefs.toSettings(currentApiKey)
            val updated = transform(current)
            // Persist API key to encrypted store (not DataStore).
            if (updated.apiKey != currentApiKey) {
                encryptedKeyStore.setApiKey(updated.apiKey)
            }
            prefs[Keys.THEME_MODE] = updated.themeMode.name
            prefs[Keys.CATEGORIES] = updated.categories.joinToString(",") { it.name }
            prefs[Keys.PURITY] = updated.purity.joinToString(",") { it.name }
            prefs[Keys.SORTING] = updated.sorting.name
            prefs[Keys.ORIENTATION] = updated.orientation.name
            prefs[Keys.DATA_SAVER_MODE] = updated.dataSaverMode
        }
    }

    private fun Preferences.toSettings(apiKey: String = ""): AppSettings {
        val defaults = AppSettings()
        val categories = this[Keys.CATEGORIES]
            ?.split(",")
            ?.mapNotNull { runCatching { Category.valueOf(it) }.getOrNull() }
            ?.toSet()
            .orEmpty()
        val purity = this[Keys.PURITY]
            ?.split(",")
            ?.mapNotNull { runCatching { Purity.valueOf(it) }.getOrNull() }
            ?.toSet()
            .orEmpty()

        return AppSettings(
            apiKey = apiKey,
            themeMode = enumOr(Keys.THEME_MODE, ThemeMode.System),
            categories = categories.ifEmpty { defaults.categories },
            purity = purity.ifEmpty { defaults.purity },
            sorting = enumOr(Keys.SORTING, Sorting.DateAdded),
            orientation = enumOr(Keys.ORIENTATION, Orientation.Both),
            dataSaverMode = this[Keys.DATA_SAVER_MODE] ?: defaults.dataSaverMode,
        )
    }

    private inline fun <reified E : Enum<E>> Preferences.enumOr(key: Preferences.Key<String>, fallback: E): E =
        runCatching { E::class.java.enumConstants?.first { it.name == this[key] } }.getOrNull() ?: fallback
}
