package com.wallkraft.app.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Assert.assertThrows

/**
 * Coverage for the type-safe navigation destinations.
 *
 * Two layers are tested:
 * - kotlinx.serialization round-trips: typed routes are @Serializable, so
 *   values with slashes, spaces, query/fragment markers or non-ASCII text,
 *   plus default values and the required Detail.id, are verified through the
 *   real serializer.
 * - [SavedStateHandle.toRoute]: only the JVM-safe cases are asserted here
 *   (absent keys fall back to defaults; arg-less destinations). Navigation
 *   decodes present args through android.os.Bundle, which is a no-op stub in
 *   plain JVM unit tests, so value-carrying handles cannot round-trip without
 *   Robolectric — on device the full path works.
 */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
class DestinationsTest {

    private val json = Json

    @Test
    fun `Browse defaults are empty strings`() {
        assertEquals(Browse(), Browse(query = "", title = ""))
    }

    @Test
    fun `Browse serialization round-trips slashes spaces unicode`() {
        val original = Browse(query = "a/b?c#d eüñ🎨&=%", title = "título / 測試 ?#")

        val decoded = json.decodeFromString(Browse.serializer(), json.encodeToString(Browse.serializer(), original))

        assertEquals(original, decoded)
    }

    @Test
    fun `Browse missing keys fall back to defaults`() {
        val viaHandle = SavedStateHandle().toRoute<Browse>()
        assertEquals("", viaHandle.query)
        assertEquals("", viaHandle.title)

        val viaJson = json.decodeFromString(Browse.serializer(), "{}")
        assertEquals("", viaJson.query)
        assertEquals("", viaJson.title)
    }

    @Test
    fun `Detail id is required`() {
        assertThrows(MissingFieldException::class.java) {
            json.decodeFromString(Detail.serializer(), "{}")
        }
    }

    @Test
    fun `Detail defaults thumb and path to empty`() {
        // Present keys cannot decode via toRoute on JVM (Bundle is stubbed),
        // so the defaulting contract is asserted via the serializer.
        val decoded = json.decodeFromString(Detail.serializer(), """{"id":"abc123"}""")

        assertEquals("abc123", decoded.id)
        assertEquals("", decoded.thumb)
        assertEquals("", decoded.path)
    }

    @Test
    fun `Detail serialization round-trips id thumb and path`() {
        val original = Detail(
            id = "x/y?z#w",
            thumb = "https://example.com/a büñ🎨.png?x=1&y=2",
            path = "/data/user/0/壁紙 full#1.png",
        )

        val decoded = json.decodeFromString(Detail.serializer(), json.encodeToString(Detail.serializer(), original))

        assertEquals(original, decoded)
    }

    @Test
    fun `Favorites and Settings are singleton destinations`() {
        assertEquals(Favorites, SavedStateHandle().toRoute<Favorites>())
        assertEquals(Settings, SavedStateHandle().toRoute<Settings>())
    }
}
