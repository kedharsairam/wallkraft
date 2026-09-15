package com.wallkraft.app.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import com.wallkraft.app.domain.model.WallpaperPosition
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SetWallpaperUseCase delegates to the [Setter] abstraction with the correct
 * [WallpaperPosition] for each convenience method.
 *
 * Note: Bitmap.createBitmap returns null in JVM unit tests (Android framework stub).
 * The Setter interface accepts any Bitmap, so we pass null to test delegation logic.
 * The actual bitmap creation and validation happens in the real Setter implementation.
 */
class SetWallpaperUseCaseTest {

    private class FakeSetter : SetWallpaperUseCase.Setter {
        var lastPosition: WallpaperPosition? = null
        var lastBitmap: Bitmap? = null
        var setResult: Boolean = true

        override suspend fun set(context: Context, bitmap: Bitmap?, position: WallpaperPosition): Boolean {
            lastBitmap = bitmap
            lastPosition = position
            return setResult
        }
    }

    @Test
    fun `setHome delegates to setter with HOME position`() = runTest {
        val setter = FakeSetter()
        val useCase = SetWallpaperUseCase(
            context = android.app.Application(),
            setter = setter,
        )

        val result = useCase.setHome(null)

        assertTrue(result)
        assertEquals(WallpaperPosition.HOME, setter.lastPosition)
    }

    @Test
    fun `setLock delegates to setter with LOCK position`() = runTest {
        val setter = FakeSetter()
        val useCase = SetWallpaperUseCase(
            context = android.app.Application(),
            setter = setter,
        )
        val result = useCase.setLock(null)

        assertTrue(result)
        assertEquals(WallpaperPosition.LOCK, setter.lastPosition)
    }

    @Test
    fun `setBoth delegates to setter with BOTH position`() = runTest {
        val setter = FakeSetter()
        val useCase = SetWallpaperUseCase(
            context = android.app.Application(),
            setter = setter,
        )

        val result = useCase.setBoth(null)

        assertTrue(result)
        assertEquals(WallpaperPosition.BOTH, setter.lastPosition)
    }

    @Test
    fun `set delegates to setter with explicit position`() = runTest {
        val setter = FakeSetter()
        val useCase = SetWallpaperUseCase(
            context = android.app.Application(),
            setter = setter,
        )

        val result = useCase.set(null, WallpaperPosition.LOCK)

        assertTrue(result)
        assertEquals(WallpaperPosition.LOCK, setter.lastPosition)
    }

    @Test
    fun `setHome returns false on failure`() = runTest {
        val setter = FakeSetter()
        setter.setResult = false
        val useCase = SetWallpaperUseCase(
            context = android.app.Application(),
            setter = setter,
        )

        val result = useCase.setHome(null)

        assertFalse(result)
    }

    @Test
    fun `setLock returns false on failure`() = runTest {
        val setter = FakeSetter()
        setter.setResult = false
        val useCase = SetWallpaperUseCase(
            context = android.app.Application(),
            setter = setter,
        )

        val result = useCase.setLock(null)

        assertFalse(result)
    }
}
