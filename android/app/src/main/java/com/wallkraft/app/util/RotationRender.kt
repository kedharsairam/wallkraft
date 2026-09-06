package com.wallkraft.app.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.wallkraft.app.domain.model.CropRect
import com.wallkraft.app.domain.model.RotationMode
import java.io.File
import kotlin.math.max

/**
 * Builds the final screen-sized wallpaper bitmap for rotation.
 *
 * - FILL: the framed crop scaled to cover the screen.
 * - SHOWCASE: sharp fit image centered on a blurred cover background —
 *   nothing is cropped and there are no black bars.
 * - ATMOSPHERE: blurred cover + dim (Muzei-style background).
 *
 * Blur is a downscale/upscale pass (bilinear smoothing) — no RenderScript,
 * no API gating, fast enough for a once-a-day background worker. All
 * failures (including OOM) yield null so the worker can try the next
 * candidate. Source files are never modified.
 */
object RotationRender {

    data class Screen(val width: Int, val height: Int)

    private val filterPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    fun render(
        sourceFile: File,
        rect: CropRect,
        screen: Screen,
        mode: RotationMode,
    ): Bitmap? {
        if (screen.width <= 0 || screen.height <= 0) return null
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(sourceFile.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            // Subsample so the decoded image is ~1-2x the screen.
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= screen.width &&
                bounds.outHeight / (sample * 2) >= screen.height
            ) {
                sample *= 2
            }
            val decode = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = BitmapFactory.decodeFile(sourceFile.absolutePath, decode)
                ?: return null
            try {
                val dw = decoded.width
                val dh = decoded.height
                if (dw <= 0 || dh <= 0) return null
                val l = (rect.left * dw).toInt().coerceIn(0, dw - 1)
                val t = (rect.top * dh).toInt().coerceIn(0, dh - 1)
                val r = (rect.right * dw).toInt().coerceIn(l + 1, dw)
                val b = (rect.bottom * dh).toInt().coerceIn(t + 1, dh)
                val cropped = Bitmap.createBitmap(decoded, l, t, r - l, b - t)
                try {
                    return when (mode) {
                        RotationMode.FILL -> cover(cropped, screen.width, screen.height)
                        RotationMode.SHOWCASE -> showcase(cropped, screen)
                        RotationMode.ATMOSPHERE -> atmosphere(cropped, screen)
                    }
                } finally {
                    cropped.recycle()
                }
            } finally {
                decoded.recycle()
            }
        } catch (_: Throwable) {
            return null
        }
    }

    private fun cover(src: Bitmap, w: Int, h: Int): Bitmap {
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawCover(Canvas(out), src, w, h)
        return out
    }

    private fun drawCover(canvas: Canvas, src: Bitmap, w: Int, h: Int) {
        val scale = max(w / src.width.toFloat(), h / src.height.toFloat())
        val dw = src.width * scale
        val dh = src.height * scale
        canvas.drawBitmap(
            src, null,
            RectF((w - dw) / 2f, (h - dh) / 2f, (w + dw) / 2f, (h + dh) / 2f),
            filterPaint,
        )
    }

    /** Downscale/upscale blur: smooth and allocation-light. */
    private fun blurred(src: Bitmap, w: Int, h: Int, factor: Int): Bitmap {
        val tw = max(1, w / factor)
        val th = max(1, h / factor)
        val tiny = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
        drawCover(Canvas(tiny), src, tw, th)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(
            tiny, null,
            RectF(0f, 0f, w.toFloat(), h.toFloat()),
            filterPaint,
        )
        tiny.recycle()
        return out
    }

    private fun showcase(cropped: Bitmap, screen: Screen): Bitmap {
        val base = blurred(cropped, screen.width, screen.height, factor = 12)
        try {
            val canvas = Canvas(base)
            // Sharp fit image centered on the blurred background.
            val fit = minOf(
                screen.width / cropped.width.toFloat(),
                screen.height / cropped.height.toFloat(),
            )
            val dw = cropped.width * fit
            val dh = cropped.height * fit
            canvas.drawBitmap(
                cropped, null,
                RectF(
                    (screen.width - dw) / 2f,
                    (screen.height - dh) / 2f,
                    (screen.width + dw) / 2f,
                    (screen.height + dh) / 2f,
                ),
                filterPaint,
            )
            return base
        } catch (t: Throwable) {
            base.recycle()
            throw t
        }
    }

    private fun atmosphere(cropped: Bitmap, screen: Screen): Bitmap {
        val base = blurred(cropped, screen.width, screen.height, factor = 16)
        try {
            // Dim so icons stay readable (Muzei-style recede).
            Canvas(base).drawColor(Color.argb(128, 0, 0, 0))
            return base
        } catch (t: Throwable) {
            base.recycle()
            throw t
        }
    }
}
