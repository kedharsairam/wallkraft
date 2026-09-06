package com.wallkraft.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wallkraft.app.util.SearchHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.searchHistoryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "search_history",
)

/**
 * Recent search queries, most-recent-first.
 *
 * A dedicated DataStore file (not AppSettings) so history writes never
 * re-emit the settings flow and recompose settings observers.
 * List logic lives in [SearchHistory] (pure, unit-tested).
 */
class SearchHistoryStore(private val context: Context) {

    private object Keys {
        val QUERIES = stringPreferencesKey("queries")
    }

    val history: Flow<List<String>> = context.searchHistoryDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> SearchHistory.deserialize(prefs[Keys.QUERIES].orEmpty()) }

    suspend fun current(): List<String> = history.first()

    suspend fun add(query: String) {
        if (query.trim().isEmpty()) return
        context.searchHistoryDataStore.edit { prefs ->
            val updated = SearchHistory.add(
                SearchHistory.deserialize(prefs[Keys.QUERIES].orEmpty()),
                query,
            )
            prefs[Keys.QUERIES] = SearchHistory.serialize(updated)
        }
    }

    suspend fun clear() {
        context.searchHistoryDataStore.edit { prefs ->
            prefs.remove(Keys.QUERIES)
        }
    }
}
