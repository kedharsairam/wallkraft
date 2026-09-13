package com.wallkraft.app.data.prefs

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.concurrent.ConcurrentHashMap

/**
 * Wraps [EncryptedSharedPreferences] for storing the API key at rest.
 *
 * Uses AES-256-GCM with a device-bound master key (Android Keystore-backed).
 * On rare emulators without hardware security, falls back to an in-memory-only
 * store (no disk persistence) so the key survives the session but not a
 * restart — the user must re-enter it. This enforces Zero Trust: the API key
 * is never stored in plaintext on disk.
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
        // Keystore unavailable (rare emulators without hardware security).
        // Zero Trust: never store the API key in plaintext on disk.
        // Use an in-memory-only store — the key survives this session but
        // is lost on restart. The user must re-enter it next launch.
        if (com.wallkraft.app.BuildConfig.DEBUG) Log.e("EncryptedApiKeyStore", "Keystore unavailable — using in-memory fallback", e)
        InMemoryPrefs()
    }

    fun getApiKey(): String = prefs.getString(KEY_API_KEY, "").orEmpty()

    fun setApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key).apply()
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply()
    }

    companion object {
        private const val KEY_API_KEY = "api_key"
    }
}

/**
 * A [SharedPreferences] implementation that stores nothing to disk.
 * Used as a Zero Trust fallback when the hardware keystore is unavailable.
 * Values live only in memory and are lost when the process dies.
 */
private class InMemoryPrefs : SharedPreferences {
    private val store = ConcurrentHashMap<String, Any?>()

    override fun getAll(): Map<String, *> = HashMap(store)
    override fun getString(key: String?, defValue: String?): String? =
        store[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        (store[key] as? Set<*>)?.filterIsInstance<String>()?.toMutableSet() ?: defValues
    override fun getInt(key: String?, defValue: Int): Int =
        (store[key] as? Int) ?: defValue
    override fun getLong(key: String?, defValue: Long): Long =
        (store[key] as? Long) ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float =
        (store[key] as? Float) ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        (store[key] as? Boolean) ?: defValue
    override fun contains(key: String?): Boolean = store.containsKey(key)
    override fun edit(): SharedPreferences.Editor = Editor()
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private inner class Editor : SharedPreferences.Editor {
        private val pending = ConcurrentHashMap<String, Any?>()
        private val removals = mutableSetOf<String>()

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) { pending[key] = value; removals.remove(key) }
            return this
        }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) { pending[key] = values; removals.remove(key) }
            return this
        }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) { pending[key] = value; removals.remove(key) }
            return this
        }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) { pending[key] = value; removals.remove(key) }
            return this
        }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) { pending[key] = value; removals.remove(key) }
            return this
        }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) { pending[key] = value; removals.remove(key) }
            return this
        }
        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) { removals.add(key); pending.remove(key) }
            return this
        }
        override fun clear(): SharedPreferences.Editor {
            pending.clear(); removals.clear(); store.clear()
            return this
        }
        override fun commit(): Boolean { apply(); return true }
        override fun apply() {
            pending.forEach { (k, v) -> store[k] = v }
            removals.forEach { store.remove(it) }
        }
    }
}
