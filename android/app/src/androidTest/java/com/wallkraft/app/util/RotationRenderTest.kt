package com.wallkraft.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wallkraft.app.domain.model.CropRect
import com.wallkraft.app.domain.model.RotationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Render engine tests on real bitmaps (needs the framework — androidTest).
 * Sources are drawn in code: left half red, right half blue.
 */
@RunWith(AndroidJUnit4::class)
class RotationRenderTest {

    private val screen = RotationRender.Screen(270, 600)

    private fun twoToneFile(w: Int = 800, h: Int = 400): File {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.RED)
        canvas.save()
        canvas.clipRect(w / 2, 0, w, h)
        canvas.drawColor(Color.BLUE)
        canvas.restore()
        val file = File.createTempFile("rotation_src", ".png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        return file
    }

    private fun brightness(pixel: Int): Int =
        Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)

    @Test
    fun fill_output_matches_screen() {
        val out = RotationRender.render(
            twoToneFile(), CropRect(0f, 0f, 1f, 1f), screen, RotationMode.FILL,
        )
        assertNotNull(out)
        assertEquals(270, out!!.width)
        assertEquals(600, out.height)
        out.recycle()
    }

    @Test
    fun showcase_output_matches_screen() {
        val out = RotationRender.render(
            twoToneFile(), CropRect(0f, 0f, 1f, 1f), screen, RotationMode.SHOWCASE,
        )
        assertNotNull(out)
        assertEquals(270, out!!.width)
        assertEquals(600, out.height)
        out.recycle()
    }

    @Test
    fun atmosphere_is_dimmer_than_showcase() {
        val file = twoToneFile()
        val show = RotationRender.render(file, CropRect(0f, 0f, 1f, 1f), screen, RotationMode.SHOWCASE)!!
        val atmo = RotationRender.render(file, CropRect(0f, 0f, 1f, 1f), screen, RotationMode.ATMOSPHERE)!!
        // Corners are blurred background in both; atmosphere adds dim.
        val corner = { b: Bitmap -> b.getPixel(5, 5) }
        assertTrue(brightness(corner(atmo)) < brightness(corner(show)))
        show.recycle()
        atmo.recycle()
        file.delete()
    }

    @Test
    fun missing_file_returns_null() {
        assertNull(
            RotationRender.render(
                File("/nonexistent/rotation.jpg"),
                CropRect(0f, 0f, 1f, 1f),
                screen,
                RotationMode.FILL,
            ),
        )
    }

    @Test
    fun degenerate_screen_returns_null() {
        assertNull(
            RotationRender.render(
                twoToneFile(),
                CropRect(0f, 0f, 1f, 1f),
                RotationRender.Screen(0, 0),
                RotationMode.FILL,
            ),
        )
    }
}
