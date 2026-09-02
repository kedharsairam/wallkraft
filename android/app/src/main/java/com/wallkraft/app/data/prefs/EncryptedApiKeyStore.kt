package com.wallkraft.app.data.prefs

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Wraps [EncryptedSharedPreferences] for storing the API key at rest.
 *
 * Uses AES-256-GCM with a device-bound master key (Android Keystore-backed).
 * Falls back to plain SharedPreferences if Keystore is unavailable (emulators
 * without hardware security), logging the failure but never crashing.
 */
class EncryptedApiKeyStore(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "wallkraft_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: Exception) {
        Log.w("EncryptedApiKeyStore", "Keystore unavailable, using fallback", e)
        // Keystore unavailable (e.g. some emulators). Fall back to plain
        // storage — still functional, just not encrypted at rest.
        context.getSharedPreferences("wallkraft_fallback_prefs", Context.MODE_PRIVATE)
    }

    fun getApiKey(): String = prefs.getString(KEY_API_KEY, "").orEmpty()

    fun setApiKey(key: String) {
        prefs.edit { putString(KEY_API_KEY, key) }
    }

    fun clearApiKey() {
        prefs.edit { remove(KEY_API_KEY) }
    }

    companion object {
        private const val KEY_API_KEY = "api_key"
    }
}
