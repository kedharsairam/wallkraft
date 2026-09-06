package com.wallkraft.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wallkraft.app.domain.model.CropRect
import com.wallkraft.app.util.RotationCrops
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.rotationCropsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "rotation_crops",
)

/**
 * Per-wallpaper crop rects, saved when the user frames a wallpaper in the
 * crop dialog. Rotation re-applies the user's own framing (see
 * RotationFraming). A dedicated DataStore file; corrupt payloads decode
 * to empty and are overwritten on the next save.
 */
class RotationCropStore(private val context: Context) {

    private object Keys {
        val CROPS = stringPreferencesKey("crops")
    }

    val crops: Flow<Map<String, CropRect>> = context.rotationCropsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> RotationCrops.decode(prefs[Keys.CROPS].orEmpty()) }

    suspend fun current(): Map<String, CropRect> = crops.first()

    suspend fun save(id: String, rect: CropRect) {
        if (!rect.isValid()) return
        context.rotationCropsDataStore.edit { prefs ->
            val updated = RotationCrops.decode(prefs[Keys.CROPS].orEmpty()) + (id to rect)
            prefs[Keys.CROPS] = RotationCrops.encode(updated)
        }
    }
}
