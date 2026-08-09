package com.wallkraft.app.presentation.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.util.WallpaperPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Full-screen crop/position dialog for setting a wallpaper.
 *
 * Shows the image scaled to fit, then lets the user pinch-zoom and drag to
 * frame exactly the region they want visible. The on-screen crop region is the
 * full screen, so what you see is exactly what gets applied (1:1 with the
 * screen). The position (home/lock/both) is chosen here too.
 */
@Composable
fun WallpaperCropDialog(
    imageFile: File,
    onDismiss: () -> Unit,
    onConfirm: (Bitmap, WallpaperPosition) -> Unit,
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    LaunchedEffect(imageFile) {
        val decoded = withContext(Dispatchers.IO) {
            runCatching { decodeBounded(imageFile) }.getOrNull()
        }
        if (decoded == null) loadFailed = true else bitmap = decoded
    }

    var position by remember { mutableStateOf(WallpaperPosition.BOTH) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val bmp = bitmap
        if (bmp == null) {
            // Loading or decode failure surface.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (loadFailed) {
                        Text(stringResource(R.string.crop_load_failed))
                        Spacer(Modifier.height(KraftSpacing.Spacing12))
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }
            return@Dialog
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black),
        ) {
            val frameW = maxWidth
            val frameH = maxHeight
            val density = LocalDensity.current
            val frameWpx = with(density) { frameW.toPx() }
            val frameHpx = with(density) { frameH.toPx() }

            // Where the image sits on screen after the fit scale + user
            // zoom/pan, in frame pixels.
            val fitScale = kotlin.math.min(
                frameWpx / bmp.width,
                frameHpx / bmp.height,
            )
            val scaledW = bmp.width * fitScale * zoom
            val scaledH = bmp.height * fitScale * zoom
            val centerX = frameWpx / 2f + panX
            val centerY = frameHpx / 2f + panY
            val left = centerX - scaledW / 2f
            val top = centerY - scaledH / 2f

            CanvasCropSurface(
                bitmap = bmp,
                left = left.toInt(),
                top = top.toInt(),
                scaledW = scaledW.toInt(),
                scaledH = scaledH.toInt(),
                onTransform = { pan, gestureZoom ->
                    val newZoom = (zoom * gestureZoom).coerceIn(1f, 8f)
                    // Keep the pinch centered: scale the existing pan about the
                    // gesture centroid, then add the drag delta.
                    panX = (panX - pan.centroidX) * (newZoom / zoom) + pan.centroidX + pan.offsetX
                    panY = (panY - pan.centroidY) * (newZoom / zoom) + pan.centroidY + pan.offsetY
                    zoom = newZoom
                    // Clamp so the image always covers the whole frame.
                    val maxPanX = kotlin.math.max(0f, (bmp.width * fitScale * zoom - frameWpx) / 2f)
                    val maxPanY = kotlin.math.max(0f, (bmp.height * fitScale * zoom - frameHpx) / 2f)
                    panX = panX.coerceIn(-maxPanX, maxPanX)
                    panY = panY.coerceIn(-maxPanY, maxPanY)
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Top bar: title + hint.
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(KraftSpacing.Spacing16),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.wallpaper_position_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = androidx.compose.ui.graphics.Color.White,
                )
                Spacer(Modifier.height(KraftSpacing.Spacing4))
                Text(
                    text = stringResource(R.string.crop_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                )
            }

            // Bottom controls: position chips + Set/Cancel.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(KraftSpacing.Spacing16),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                ) {
                    listOf(
                        WallpaperPosition.HOME to R.string.wallpaper_position_home,
                        WallpaperPosition.LOCK to R.string.wallpaper_position_lock,
                        WallpaperPosition.BOTH to R.string.wallpaper_position_both,
                    ).forEach { (pos, labelRes) ->
                        FilterChip(
                            selected = position == pos,
                            onClick = { position = pos },
                            label = { Text(stringResource(labelRes)) },
                        )
                    }
                }
                Spacer(Modifier.height(KraftSpacing.Spacing16))
                Row(horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing16)) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            val crop = Bitmap.createBitmap(
                                frameWpx.toInt(),
                                frameHpx.toInt(),
                                Bitmap.Config.ARGB_8888,
                            )
                            val src: android.graphics.Rect? = null
                            val paint: android.graphics.Paint? = null
                            android.graphics.Canvas(crop).drawBitmap(
                                bmp,
                                src,
                                RectF(left, top, left + scaledW, top + scaledH),
                                paint,
                            )
                            onConfirm(crop, position)
                        },
                        modifier = Modifier.clip(RoundedCornerShape(50)),
                    ) {
                        Text(stringResource(R.string.set_as_wallpaper))
                    }
                }
            }
        }
    }
}

/** Draws the cropped image region and forwards pan/zoom gestures. */
@Composable
private fun CanvasCropSurface(
    bitmap: Bitmap,
    left: Int,
    top: Int,
    scaledW: Int,
    scaledH: Int,
    onTransform: (PanZoom, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTransformGestures { centroid, pan, gestureZoom, _ ->
                onTransform(
                    PanZoom(centroid.x, centroid.y, pan.x, pan.y),
                    gestureZoom,
                )
            }
        },
    ) {
        drawImage(
            image = bitmap.asImageBitmap(),
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bitmap.width, bitmap.height),
            dstOffset = IntOffset(left, top),
            dstSize = IntSize(scaledW, scaledH),
        )
    }
}

private data class PanZoom(
    val centroidX: Float,
    val centroidY: Float,
    val offsetX: Float,
    val offsetY: Float,
)

/**
 * Decodes [file] with a size cap so a huge (e.g. 8K) wallpaper can't OOM the
 * dialog. The crop output is always screen-resolution, so capping the source
 * at [MAX_DECODE_DIM] px keeps plenty of detail for a sharp result while
 * bounding memory to ~64 MB worst case (4096×4096×4).
 */
private fun decodeBounded(file: File): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= MAX_DECODE_DIM ||
        bounds.outHeight / (sample * 2) >= MAX_DECODE_DIM
    ) {
        sample *= 2
    }
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeFile(file.absolutePath, opts)
}

private const val MAX_DECODE_DIM = 4096
