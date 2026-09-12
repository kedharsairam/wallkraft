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
        val enc = EncryptedSharedPreferences.create(
            context,
            "wallkraft_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        // Encrypted succeeded — clean up any prior plaintext fallback file.
        try { context.deleteSharedPreferences("wallkraft_fallback_prefs") } catch (_: Exception) {}
        enc
    } catch (e: Exception) {
        // Keystore unavailable (emulators without hardware). Plain fallback
        // is P0 insecure — log error (DEBUG only) and warn UI via Settings.
        if (com.wallkraft.app.BuildConfig.DEBUG) Log.e("EncryptedApiKeyStore", "Keystore unavailable, using unencrypted fallback (insecure)", e)
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
