package com.wallkraft.app.util

import android.app.WallpaperManager
import com.wallkraft.app.domain.model.WallpaperPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperActionsDataTest {

    @Test
    fun `WallpaperPosition HOME has correct flags`() {
        assertEquals(WallpaperManager.FLAG_SYSTEM, WallpaperPosition.HOME.flags)
    }

    @Test
    fun `WallpaperPosition LOCK has correct flags`() {
        assertEquals(WallpaperManager.FLAG_LOCK, WallpaperPosition.LOCK.flags)
    }

    @Test
    fun `WallpaperPosition BOTH has combined flags`() {
        val expected = WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        assertEquals(expected, WallpaperPosition.BOTH.flags)
    }

    @Test
    fun `WallpaperPosition enum has exactly 3 values`() {
        assertEquals(3, WallpaperPosition.values().size)
        assertTrue(WallpaperPosition.values().contains(WallpaperPosition.HOME))
        assertTrue(WallpaperPosition.values().contains(WallpaperPosition.LOCK))
        assertTrue(WallpaperPosition.values().contains(WallpaperPosition.BOTH))
    }
}
