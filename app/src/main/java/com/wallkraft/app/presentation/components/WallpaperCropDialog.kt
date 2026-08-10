package com.wallkraft.app.presentation.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.util.WallpaperPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    onConfirm: suspend (Bitmap, WallpaperPosition) -> Boolean,
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
    val scope = rememberCoroutineScope()
    var animJob by remember { mutableStateOf<Job?>(null) }
    // Applying state: true while the wallpaper is being set (button shows a
    // spinner and is disabled). setResult is null until the apply finishes,
    // then true (success → centered checkmark, then dismiss) or false
    // (failure → snackbar, dialog stays open to retry).
    var applying by remember { mutableStateOf(false) }
    var setResult by remember { mutableStateOf<Boolean?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val setFailedMsg = stringResource(R.string.wallpaper_set_failed)

    // The dialog window extends behind the navigation bar but does not dispatch
    // the nav-bar inset to its content (navigationBarsPadding() reads 0 here).
    // Read the real insets from the activity window, which is edge-to-edge and
    // reports them correctly, and pad the bottom controls explicitly.
    //
    // The dialog window is forced edge-to-edge below (see the LaunchedEffect
    // inside the Dialog), so the crop surface spans the full screen and the
    // image centers on the visible screen without any offset. The controls sit
    // at the bottom of the (taller-than-frame) crop surface, so the bottom
    // padding must clear the navigation bar.
    val density = LocalDensity.current
    val context = LocalContext.current
    val statusTopPx = remember {
        val activity = context.findActivity()
        activity?.window?.decorView
            ?.let { ViewCompat.getRootWindowInsets(it) }
            ?.getInsets(WindowInsetsCompat.Type.statusBars())
            ?.top ?: 0
    }
    val navBottomPx = remember {
        val activity = context.findActivity()
        activity?.window?.decorView
            ?.let { ViewCompat.getRootWindowInsets(it) }
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())
            ?.bottom ?: 0
    }
    val bottomPadding = with(density) { (navBottomPx + statusTopPx).toDp() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        // DialogProperties.decorFitsSystemWindows alone leaves the dialog
        // window inset below the status bar on this device (frame starts at
        // screen y=statusTopPx), which would leave a black band at the top of
        // the crop surface in fill state. Force the window edge-to-edge so the
        // canvas spans the full screen and the crop output matches it 1:1.
        val dialogView = LocalView.current
        LaunchedEffect(Unit) {
            var v: View? = dialogView
            while (v != null) {
                if (v is DialogWindowProvider) {
                    val window = v.window
                    window.setDecorFitsSystemWindows(false)
                    val lp = window.attributes
                    lp.gravity = android.view.Gravity.TOP or android.view.Gravity.START
                    lp.width = WindowManager.LayoutParams.MATCH_PARENT
                    lp.height = WindowManager.LayoutParams.MATCH_PARENT
                    lp.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    // The activity window carries these flags and is laid out
                    // edge-to-edge; the dialog window is missing them, so the
                    // window manager insets it below the status bar. Add them so
                    // the crop surface spans the full screen like the detail page.
                    lp.flags = lp.flags or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                    window.attributes = lp
                    break
                }
                v = v.parent as? View
            }
        }

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
            // The minimum zoom where the image covers the whole frame — for a
            // landscape wallpaper that's top-to-bottom fill (height matches the
            // screen, width overflows), for portrait it's left-to-right fill.
            // At this level one axis is exactly flush, so that axis can't pan
            // (nothing beyond it) while the other still can.
            val fillZoom = kotlin.math.max(
                frameWpx / (bmp.width * fitScale),
                frameHpx / (bmp.height * fitScale),
            ).coerceIn(1f, MAX_CROP_ZOOM)

            fun animateTo(targetZoom: Float, targetPanX: Float, targetPanY: Float) {
                animJob?.cancel()
                val startZoom = zoom
                val startPanX = panX
                val startPanY = panY
                animJob = scope.launch {
                    val startTime = withFrameNanos { it }
                    while (true) {
                        val now = withFrameNanos { it }
                        val t = ((now - startTime) / 1_000_000L).toFloat() / ANIM_DURATION_MS.toFloat()
                        val eased = FastOutSlowInEasing.transform(t.coerceIn(0f, 1f))
                        zoom = lerp(startZoom, targetZoom, eased)
                        panX = lerp(startPanX, targetPanX, eased)
                        panY = lerp(startPanY, targetPanY, eased)
                        if (t >= 1f) break
                    }
                }
            }

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
                    animJob?.cancel()
                    val newZoom = (zoom * gestureZoom).coerceIn(1f, MAX_CROP_ZOOM)
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
                onDoubleTap = {
                    // Toggle: at fit → zoom to fill-frame (centered); zoomed →
                    // return to fit. Mirrors the detail screen's first double-tap.
                    if (zoom <= 1.01f) {
                        animateTo(fillZoom, 0f, 0f)
                    } else {
                        animateTo(1f, 0f, 0f)
                    }
                },
                modifier = Modifier
                    .fillMaxSize(),
            )

            // Top scrim: darkens the top of the screen so the white title/hint
            // (and the status bar icons) stay readable on light wallpapers.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color.Transparent,
                            ),
                        ),
                    ),
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
                    color = Color.White,
                )
                Spacer(Modifier.height(KraftSpacing.Spacing4))
                Text(
                    text = stringResource(R.string.crop_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }

            // Bottom scrim: darkens the bottom of the screen so the chips and
            // buttons stay readable on light wallpapers.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.45f),
                            ),
                        ),
                    ),
            )

            // Bottom controls: position chips + Set/Cancel.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = bottomPadding)
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
                    TextButton(onClick = onDismiss, enabled = !applying) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            if (applying) return@Button
                            applying = true
                            scope.launch {
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
                                val ok = onConfirm(crop, position)
                                if (ok) {
                                    setResult = true
                                    delay(1200)
                                    onDismiss()
                                } else {
                                    applying = false
                                    snackbarHostState.showSnackbar(setFailedMsg)
                                }
                            }
                        },
                        modifier = Modifier.clip(RoundedCornerShape(50)),
                        enabled = !applying,
                    ) {
                        if (applying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(stringResource(R.string.set_as_wallpaper))
                        }
                    }
                }
            }

            // Failure feedback: snackbar above the controls, dialog stays open.
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomPadding)
                    .padding(bottom = KraftSpacing.Spacing64),
            )

            // Success feedback: centered checkmark + message, then auto-dismiss.
            if (setResult == true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(72.dp),
                        )
                        Spacer(Modifier.height(KraftSpacing.Spacing12))
                        Text(
                            text = stringResource(R.string.wallpaper_set),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

/** Draws the cropped image region and forwards pan/zoom/double-tap gestures. */
@Composable
private fun CanvasCropSurface(
    bitmap: Bitmap,
    left: Int,
    top: Int,
    scaledW: Int,
    scaledH: Int,
    onTransform: (PanZoom, Float) -> Unit,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, gestureZoom, _ ->
                    onTransform(
                        PanZoom(centroid.x, centroid.y, pan.x, pan.y),
                        gestureZoom,
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap() },
                )
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
private const val MAX_CROP_ZOOM = 8f
private const val ANIM_DURATION_MS = 220L

/** Walks up the context chain to the owning [Activity], if any. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
