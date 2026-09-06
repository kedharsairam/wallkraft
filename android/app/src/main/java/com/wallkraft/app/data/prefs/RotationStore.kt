package com.wallkraft.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wallkraft.app.domain.model.RotationMode
import com.wallkraft.app.domain.model.RotationSchedule
import com.wallkraft.app.domain.model.RotationTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.rotationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "rotation",
)

/** Rotation settings snapshot. */
data class RotationSettings(
    val schedule: RotationSchedule = RotationSchedule.OFF,
    val mode: RotationMode = RotationMode.SHOWCASE,
    val target: RotationTarget = RotationTarget.BOTH,
    /** Null = all favorites; otherwise a collection id. */
    val sourceCollectionId: Long? = null,
    /** Index of the last applied wallpaper (round-robin cursor). */
    val lastIndex: Int = -1,
)

/**
 * Wallpaper rotation settings + cursor.
 *
 * A dedicated DataStore file (not AppSettings) so schedule writes never
 * re-emit the settings flow.
 */
class RotationStore(private val context: Context) {

    private object Keys {
        val SCHEDULE = stringPreferencesKey("schedule")
        val MODE = stringPreferencesKey("mode")
        val TARGET = stringPreferencesKey("target")
        val SOURCE_COLLECTION = longPreferencesKey("source_collection")
        val LAST_INDEX = intPreferencesKey("last_index")
    }

    val settings: Flow<RotationSettings> = context.rotationDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            RotationSettings(
                schedule = runCatching {
                    RotationSchedule.valueOf(prefs[Keys.SCHEDULE]!!)
                }.getOrDefault(RotationSchedule.OFF),
                mode = runCatching {
                    RotationMode.valueOf(prefs[Keys.MODE]!!)
                }.getOrDefault(RotationMode.SHOWCASE),
                target = runCatching {
                    RotationTarget.valueOf(prefs[Keys.TARGET]!!)
                }.getOrDefault(RotationTarget.BOTH),
                sourceCollectionId = prefs[Keys.SOURCE_COLLECTION]?.takeIf { it >= 0 },
                lastIndex = prefs[Keys.LAST_INDEX] ?: -1,
            )
        }

    suspend fun current(): RotationSettings = settings.first()

    suspend fun setSchedule(schedule: RotationSchedule) {
        context.rotationDataStore.edit { it[Keys.SCHEDULE] = schedule.name }
    }

    suspend fun setMode(mode: RotationMode) {
        context.rotationDataStore.edit { it[Keys.MODE] = mode.name }
    }

    suspend fun setTarget(target: RotationTarget) {
        context.rotationDataStore.edit { it[Keys.TARGET] = target.name }
    }

    suspend fun setSourceCollection(id: Long?) {
        context.rotationDataStore.edit { prefs ->
            if (id == null) prefs.remove(Keys.SOURCE_COLLECTION)
            else prefs[Keys.SOURCE_COLLECTION] = id
        }
    }

    suspend fun setLastIndex(index: Int) {
        context.rotationDataStore.edit { it[Keys.LAST_INDEX] = index }
    }
}
