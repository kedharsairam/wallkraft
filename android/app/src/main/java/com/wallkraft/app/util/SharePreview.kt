package com.wallkraft.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.wallkraft.app.R
import com.wallkraft.app.domain.model.Wallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Generates branded share-preview bitmaps for social sharing.
 *
 * Composites the wallpaper image with a branded bar at the bottom
 * and saves it as a content:// Uri via FileProvider.
 */
object SharePreview {

    private const val MIN_WIDTH = 1080
    // Output is a compressed share card, not the wallpaper itself: cap the
    // width so a 4K source can't spike ~100MB+ transient bitmaps on 2GB
    // devices. 1080x1620 (~7MB ARGB_8888) is plenty for a share preview.
    private const val MAX_WIDTH = 1080
    // Bounded cache: share previews are transient share-sheet inputs.
    private const val MAX_CACHED_PREVIEWS = 20
    private const val QUALITY = 90
    private const val CORNER_RADIUS_DP = 16f
    private const val BAR_HEIGHT_RATIO = 0.3f
    private const val BRAND_TEXT_SIZE_SP = 18f
    private const val URL_TEXT_SIZE_SP = 12f
    private const val DIVIDER_HEIGHT_PX = 2
    private const val BRAND_PADDING_DP = 24f
    private const val DENSITY_SCALE = 3f // assumed density for dp->px

    /**
     * Creates a composite share-preview bitmap and returns a content:// Uri.
     *
     * Layout:
     * - Top 70%: wallpaper image (full width, cropped to fill)
     * - Bottom 30%: branded bar with AuroraBlue background
     *   - "WallKraft" text (white, bold, 18sp)
     *   - "wallkraft.app" text (lighter white, 12sp)
     *   - Subtle divider line
     *
     * Output is saved to cache dir as JPEG with rounded corners.
     * Falls back to null on any error so the caller can share the raw bitmap.
     */
    suspend fun generateSharePreview(
        context: Context,
        wallpaper: Wallpaper,
        bitmap: Bitmap,
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val width = bitmap.width.coerceIn(MIN_WIDTH, MAX_WIDTH)
            val height = (width * 1.5f).toInt() // 2:3 portrait-ish ratio

            val scaledBitmap = if (bitmap.width != width || bitmap.height != height) {
                Bitmap.createScaledBitmap(bitmap, width, height, true)
            } else {
                bitmap.copy(Bitmap.Config.ARGB_8888, true)
            }

            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)

            val density = DENSITY_SCALE
            val cornerRadiusPx = CORNER_RADIUS_DP * density

            // Clip to rounded rectangle
            val clipPath = Path()
            clipPath.addRoundRect(
                0f, 0f, width.toFloat(), height.toFloat(),
                cornerRadiusPx, cornerRadiusPx,
                Path.Direction.CW,
            )
            canvas.clipPath(clipPath)

            // Draw wallpaper in top 70%
            val barHeight = (height * BAR_HEIGHT_RATIO).toInt()
            val wallpaperHeight = height - barHeight
            val wallpaperRect = RectF(0f, 0f, width.toFloat(), wallpaperHeight.toFloat())
            canvas.drawBitmap(scaledBitmap, null, wallpaperRect, null)

            if (scaledBitmap !== bitmap) {
                scaledBitmap.recycle()
            }

            // Draw branded bar background (AuroraBlue #0A84FF)
            val barPaint = Paint().apply {
                color = Color.parseColor("#0A84FF")
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, wallpaperHeight.toFloat(), width.toFloat(), height.toFloat(), barPaint)

            // Divider line
            val dividerPaint = Paint().apply {
                color = Color.WHITE
                alpha = 60
                style = Paint.Style.FILL
            }
            canvas.drawRect(
                0f, wallpaperHeight.toFloat(),
                width.toFloat(), (wallpaperHeight + DIVIDER_HEIGHT_PX).toFloat(),
                dividerPaint,
            )

            // Brand text
            val paddingPx = BRAND_PADDING_DP * density
            val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = Typeface.DEFAULT_BOLD
                textSize = BRAND_TEXT_SIZE_SP * density
            }
            val brandText = context.getString(R.string.share_preview_brand)
            val brandY = wallpaperHeight + barHeight * 0.45f
            canvas.drawText(brandText, paddingPx, brandY, brandPaint)

            // URL text
            val urlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = 180
                textSize = URL_TEXT_SIZE_SP * density
            }
            val urlText = context.getString(R.string.share_preview_url)
            val urlY = brandY + URL_TEXT_SIZE_SP * density * 1.4f
            canvas.drawText(urlText, paddingPx, urlY, urlPaint)

            // Save to cache
            val dir = File(context.cacheDir, "share_cache").apply { mkdirs() }
            val file = File(dir, "share_preview_${wallpaper.id}.jpg")
            FileOutputStream(file).use { out ->
                result.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            }
            result.recycle()
            pruneCache(dir)

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        } catch (_: Exception) {
            null
        }
    }

    /** Evicts oldest previews beyond [MAX_CACHED_PREVIEWS]. Call on a worker thread. */
    private fun pruneCache(dir: File) {
        runCatching {
            dir.listFiles()
                ?.filter { it.isFile }
                ?.sortedBy { it.lastModified() }
                ?.dropLast(MAX_CACHED_PREVIEWS)
                ?.forEach { it.delete() }
        }
    }
}
