package com.wallkraft.app

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Proves every shipped locale file parses and resolves core strings.
 *
 * Catches missing/broken locale strings.xml files at test time instead of
 * in users hands.
 */
@RunWith(AndroidJUnit4::class)
class LocalizationTest {

    private fun stringFor(localeTag: String, resId: Int): String {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(base.resources.configuration)
        config.setLocale(Locale.forLanguageTag(localeTag))
        return base.createConfigurationContext(config).resources.getString(resId)
    }

    @Test
    fun spanish_resolves() {
        assertEquals("Explorar", stringFor("es", R.string.tab_browse))
        assertEquals("Ajustes", stringFor("es", R.string.tab_settings))
        assertEquals("Azul", stringFor("es", R.string.color_blue))
    }

    @Test
    fun portuguese_resolves() {
        assertEquals("Explorar", stringFor("pt", R.string.tab_browse))
        assertEquals("Configurações", stringFor("pt", R.string.tab_settings))
        assertEquals("Azul", stringFor("pt", R.string.color_blue))
    }

    @Test
    fun hindi_resolves() {
        assertEquals("ब्राउज़", stringFor("hi", R.string.tab_browse))
        assertEquals("सेटिंग", stringFor("hi", R.string.tab_settings))
        assertEquals("नीला", stringFor("hi", R.string.color_blue))
    }

    @Test
    fun japanese_resolves() {
        assertEquals("探す", stringFor("ja", R.string.tab_browse))
        assertEquals("設定", stringFor("ja", R.string.tab_settings))
        assertEquals("青", stringFor("ja", R.string.color_blue))
    }

    @Test
    fun english_unchanged() {
        assertEquals("Browse", stringFor("en", R.string.tab_browse))
        assertEquals("Settings", stringFor("en", R.string.tab_settings))
    }
}
