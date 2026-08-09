package com.wallkraft.app.presentation.detail

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil3.compose.AsyncImage
import com.wallkraft.app.AppContainer
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftColors
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.presentation.components.ErrorState
import com.wallkraft.app.presentation.components.WallpaperCropDialog
import com.wallkraft.app.presentation.components.ZoomableImage
import com.wallkraft.app.util.WallpaperActions
import com.wallkraft.app.util.WallpaperPosition
import com.wallkraft.app.util.toUserMessage
import com.wallkraft.app.util.wallpaperCategoryLabel
import java.io.File
import kotlinx.coroutines.launch
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    container: AppContainer,
    wallpaperId: String,
    onBack: () -> Unit,
    onTagClick: (String) -> Unit = {},
    onZoomChanged: (Boolean) -> Unit = {},
    navBarPadding: androidx.compose.ui.unit.Dp = 0.dp,
    previewThumb: String = "",
    previewPath: String = "",
) {
    val viewModel: DetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                DetailViewModel(
                    id = wallpaperId,
                    wallpaperRepository = container.wallpaperRepository,
                    favoritesRepository = container.favoritesRepository,
                    errorMessage = { e -> e.toUserMessage(container.resources) },
                    previewThumb = previewThumb.ifBlank { null },
                    previewPath = previewPath.ifBlank { null },
                )
            }
        },
    )
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val wallpaper = uiState.wallpaper
    var setWallpaperTarget by remember { mutableStateOf<Wallpaper?>(null) }

    // Data saver: when enabled, the full-res image is deferred until the user
    // zooms (or the image is already local — favorites cost zero data). The
    // thumbnail still renders instantly, so the screen never feels slow.
    // Read once (null until known) so the full-res decision is never made
    // against the default value — that would start a download we don't want.
    var dataSaverEnabled by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        dataSaverEnabled = container.settings.current().dataSaverMode
    }

    // Resolve snackbar copy now — stringResource is composable and can't
    // be called inside the action callbacks below.
    val wallpaperSetMsg = stringResource(R.string.wallpaper_set)
    val wallpaperSetFailedMsg = stringResource(R.string.wallpaper_set_failed)

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null && wallpaper == null -> ErrorState(
                message = uiState.error ?: "",
                onRetry = viewModel::load,
                modifier = Modifier.fillMaxSize(),
            )
            wallpaper != null -> {
                if (wallpaper.path.isBlank()) {
                    ErrorState(
                        message = stringResource(R.string.wallpaper_set_failed),
                        onRetry = viewModel::load,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    DetailContent(
                        wallpaper = wallpaper,
                        isFavorite = wallpaper.id in uiState.favoriteIds,
                        dataSaverEnabled = dataSaverEnabled,
                        // Prefer the locally-cached favorite copy when it exists
                        // so the image loads instantly and works offline; fall
                        // back to the network URL otherwise.
                        imageModel = container.favoriteImageStore.fileFor(wallpaper.id) ?: wallpaper.path,
                        onToggleFavorite = {
                            // Subtle tick so the action feels acknowledged.
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val wasFavorite = wallpaper.id in uiState.favoriteIds
                            viewModel.toggleFavorite(wallpaper)
                            // Keep favorites viewable offline: download the
                            // full-res into the private store on favorite,
                            // remove it on unfavorite. Fire-and-forget so the
                            // toggle never waits on the network.
                            if (wasFavorite) {
                                container.favoriteImageStore.delete(wallpaper.id)
                            } else {
                                scope.launch { container.favoriteImageStore.save(wallpaper) }
                            }
                        },
                        onDownload = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            WallpaperActions.download(context, wallpaper)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.downloading, wallpaper.resolution),
                                )
                            }
                        },
                        onSetWallpaper = { setWallpaperTarget = wallpaper },
                        onShare = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch {
                                WallpaperActions.share(
                                    context,
                                    wallpaper,
                                    container.okHttpClient,
                                    container.favoriteImageStore.fileFor(wallpaper.id),
                                )
                            }
                        },
                        onBack = onBack,
                        onTagClick = onTagClick,
                        onZoomChanged = onZoomChanged,
                        navBarPadding = navBarPadding,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

val setTarget = setWallpaperTarget
    if (setTarget != null) {
        // Resolve a local image file (offline favorite copy, else download to
        // cache) so the crop dialog can decode it. Show the crop dialog once
        // it's ready; surface an error if the image can't be obtained.
        var resolvedFile by remember(setTarget) { mutableStateOf<File?>(null) }
        var resolving by remember(setTarget) { mutableStateOf(true) }
        LaunchedEffect(setTarget) {
            resolving = true
            resolvedFile = WallpaperActions.imageFile(
                context,
                setTarget,
                container.okHttpClient,
                container.favoriteImageStore.fileFor(setTarget.id),
            )
            resolving = false
        }
        val file = resolvedFile
        when {
            file != null -> WallpaperCropDialog(
                imageFile = file,
                onDismiss = { setWallpaperTarget = null },
                onConfirm = { cropped, position ->
                    setWallpaperTarget = null
                    scope.launch {
                        val ok = WallpaperActions.setAsWallpaper(context, cropped, position)
                        if (ok) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        snackbarHostState.showSnackbar(
                            if (ok) wallpaperSetMsg else wallpaperSetFailedMsg
                        )
                    }
                },
            )
            resolving -> {
                // Still resolving the image (downloading a non-favorite's
                // full-res into cache). Show a spinner so the tap isn't a
                // silent no-op while the network does its thing.
                Dialog(
                    onDismissRequest = { setWallpaperTarget = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            else -> {
                // Resolution finished but no file — show the failure and close.
                LaunchedEffect(Unit) {
                    setWallpaperTarget = null
                    snackbarHostState.showSnackbar(wallpaperSetFailedMsg)
                }
            }
        }
    }
}

/**
 * A single full-bleed surface in the Instagram-reels style. The wallpaper
 * fills the entire screen, dead center. A top bar (back + open) floats at the
 * top; a vertical stack of action buttons (favorite, download, set wallpaper)
 * floats on the right edge; and a bottom overlay shows metadata + clickable
 * tags. All chrome fades away while zoomed.
 *
 * Pinch-to-zoom and double-tap work in place — no separate screen. The first
 * double-tap fills the image top-to-bottom exactly, the second zooms further,
 * the third returns.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    wallpaper: Wallpaper,
    isFavorite: Boolean,
    dataSaverEnabled: Boolean?,
    imageModel: Any,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    onSetWallpaper: () -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit,
    onTagClick: (String) -> Unit,
    onZoomChanged: (Boolean) -> Unit,
    navBarPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var isZoomed by remember { mutableStateOf(false) }

    // Hide the system bars while zoomed so the image is truly edge-to-edge.
    val window = (context as? Activity)?.window
    if (window != null) {
        val controller = remember(window) { WindowInsetsControllerCompat(window, window.decorView) }
        DisposableEffect(controller, isZoomed) {
            if (isZoomed) controller.hide(WindowInsetsCompat.Type.systemBars())
            else controller.show(WindowInsetsCompat.Type.systemBars())
            onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
        }
        DisposableEffect(window, isZoomed) {
            if (isZoomed) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        }
    }

    val w = wallpaper.dimensionX.toFloat().takeIf { it > 0f } ?: 1f
    val h = wallpaper.dimensionY.toFloat().takeIf { it > 0f } ?: 1f
    val aspect = w / h

    BoxWithConstraints(modifier = modifier.background(Color.Black)) {
        val viewW = maxWidth.value
        val viewH = maxHeight.value
        // Exact fill scale: at 1x the image (ContentScale.Fit) shows at
        // min(viewW/aspect, viewH) tall; scaling by this makes its height
        // exactly the viewport height — top-to-bottom, never beyond.
        val fillScale = if (aspect <= 0f) 1f else {
            val displayedH = min(viewW / aspect, viewH)
            (viewH / displayedH).coerceAtLeast(1f).coerceAtMost(8f)
        }
        // First double-tap must always produce a visible zoom. If the image is
        // smaller than the screen (landscape), fill it top-to-bottom exactly.
        // If it already fills the screen (portrait), zoom in 2x instead — a
        // fill scale of 1.0 would otherwise make the first double-tap a no-op.
        val firstLevel = if (fillScale > 1.05f) fillScale else 2f
        // Second double-tap = 1:1 native resolution: scale the image so each
        // image pixel maps to one screen pixel. The image is displayed at
        // `displayedH` tall at 1x (fit); to show it at its real pixel height
        // we scale by nativeH / displayedH. Clamped so it always zooms deeper
        // than the fill level (never a no-op or a zoom-out).
        val displayedH = if (aspect <= 0f) viewH else min(viewW / aspect, viewH)
        val resolutionLevel = if (aspect <= 0f || displayedH <= 0f) {
            (firstLevel * 2f).coerceAtMost(8f)
        } else {
            (h / displayedH).coerceIn(firstLevel * 1.2f, 8f)
        }
        val zoomLevels = remember(viewW, viewH, resolutionLevel) {
            listOf(firstLevel, resolutionLevel, 1f)
        }

        // -- No hero transition: the detail opens instantly showing the
        // thumbnail, and the full-res image loads right away behind a subtle
        // top loading bar. Simple and predictable — no animation to get wrong.
        var fullResLoaded by remember { mutableStateOf(false) }
        val heroScope = rememberCoroutineScope()
        // Data saver defers the full-res download until the user zooms (the
        // moment they actually need the detail). A local file — like the
        // offline favorite copy — costs zero data, so it loads immediately.
        // Keyed on [imageModel] and [dataSaverEnabled] so it flips to true if
        // a local copy appears while the screen is open (e.g. favoriting
        // mid-view). While the setting is still unknown (null) we defer —
        // never start a download against the default.
        var fullResRequested by remember(imageModel, dataSaverEnabled) {
            mutableStateOf(dataSaverEnabled == false || imageModel is File)
        }
        // Determinate progress 0f..1f. Coil doesn't expose real download
        // progress, so we animate it smoothly toward 1 and snap to 1 the
        // instant the full-res image is ready — a clean left-to-right fill.
        // Keyed on [fullResRequested] so the bar restarts from 0 when data
        // saver defers the load until the user zooms.
        val fullResProgress = remember { Animatable(0f) }
        LaunchedEffect(fullResRequested) {
            if (fullResRequested) {
                fullResProgress.snapTo(0f)
                fullResProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(1500, easing = LinearEasing),
                )
            }
        }

        ZoomableImage(
            model = imageModel,
            placeholderModel = wallpaper.thumbnail,
            contentDescription = wallpaper.resolution,
            zoomLevels = zoomLevels,
            onZoomChanged = { newScale ->
                val zoomed = newScale > 1.01f
                // First zoom is the user asking for detail — request the
                // full-res image now (data saver). Harmless when already on.
                if (zoomed) fullResRequested = true
                isZoomed = zoomed
                onZoomChanged(zoomed)
            },
            onLoaded = {
                fullResLoaded = true
                heroScope.launch { fullResProgress.snapTo(1f) }
            },
            loadFullRes = fullResRequested,
            modifier = Modifier.fillMaxSize(),
        )

        // Subtle loading bar at the very top while the full-res image loads.
        // Determinate: fills left-to-right from 0 to 100, then fades out.
        // Only shown when the full-res is actually being requested — in data
        // saver mode that's once the user zooms, not on open.
        AnimatedVisibility(
            visible = fullResRequested && !fullResLoaded && !isZoomed,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
        ) {
            LinearProgressIndicator(
                progress = { fullResProgress.value },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = KraftColors.AccentGreen,
                trackColor = Color.Transparent,
            )
        }

        // Data saver: while zoomed before the full-res has arrived, show a
        // small "loading full resolution" pill so the blurry thumbnail never
        // reads as a broken image. Fades out the instant the image is ready.
        AnimatedVisibility(
            visible = isZoomed && fullResRequested && !fullResLoaded,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = KraftSpacing.Spacing24),
        ) {
            Surface(
                shape = RoundedCornerShape(KraftRadius.Small),
                color = Color.Black.copy(alpha = 0.6f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = KraftSpacing.Spacing12, vertical = KraftSpacing.Spacing8),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                    Spacer(Modifier.width(KraftSpacing.Spacing8))
                    Text(
                        text = stringResource(R.string.loading_full_resolution),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                    )
                }
            }
        }

        // Top bar — back + open. Fades away when zoomed.
        AnimatedVisibility(
            visible = !isZoomed,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)))
                    .statusBarsPadding(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = KraftSpacing.Spacing4, vertical = KraftSpacing.Spacing4),
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { WallpaperActions.openInBrowser(context, wallpaper) }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = stringResource(R.string.open_on_wallhaven), tint = Color.White)
                    }
                }
            }
        }

        // Invisible measurement pass. These sit at the top-start corner, are
        // fully transparent, and their chips are non-clickable, so they never
        // intercept touches — they exist only to size the bottom panel.
        var collapsedContentHeight by remember { mutableStateOf(0) }
        var fullContentHeight by remember { mutableStateOf(0) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(0f)
                .onSizeChanged { collapsedContentHeight = it.height },
        ) {
            DetailPanelContent(
                wallpaper = wallpaper,
                onTagClick = onTagClick,
                clickable = false,
                collapsed = true,
                bottomPadding = navBarPadding,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .alpha(0f)
                .onSizeChanged { fullContentHeight = it.height },
        ) {
            DetailPanelContent(
                wallpaper = wallpaper,
                onTagClick = onTagClick,
                clickable = false,
                collapsed = false,
                bottomPadding = navBarPadding,
            )
        }

        val density = LocalDensity.current
        val collapsedHeightPx = collapsedContentHeight.toFloat()
        val maxPanelHeightPx = min(fullContentHeight.toFloat(), maxHeight.value)

        // Two-state panel: collapsed (metadata + one truncated tag line with an
        // inline "...") or expanded (all tag chips). Swipe up to expand, swipe
        // down to collapse. While dragging the panel follows the finger; on
        // release it settles to whichever state is closer.
        var expanded by remember(wallpaper.id) { mutableStateOf(false) }
        val panelHeight = remember(wallpaper.id) { Animatable(0f) }
        // Drag offset in px, tracked synchronously so the release decision is
        // correct even though the Animatable settles asynchronously.
        var dragOffsetPx by remember(wallpaper.id) { mutableStateOf(0f) }
        // Displayed height = settled height + live drag offset, clamped.
        val displayedHeightPx = (panelHeight.value + dragOffsetPx)
            .coerceIn(collapsedHeightPx, maxPanelHeightPx)
        val scope = rememberCoroutineScope()

        // Settle the panel to the target on first layout only.
        LaunchedEffect(collapsedHeightPx, wallpaper.id) {
            if (collapsedHeightPx > 0f && panelHeight.value == 0f) {
                panelHeight.snapTo(collapsedHeightPx)
            }
        }

        // Right-edge vertical action stack (favorite, download, set wallpaper),
        // like Instagram reels. Floats just above the collapsed panel and hides
        // while the panel is expanded. Fades away when zoomed.
        AnimatedVisibility(
            visible = !isZoomed && !expanded,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = KraftSpacing.Spacing12)
                .padding(bottom = with(density) { panelHeight.value.toDp() } + KraftSpacing.Spacing16),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing16),
            ) {
                ReelsActionButton(
                    onClick = onToggleFavorite,
                    icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites),
                    tint = if (isFavorite) KraftColors.AccentRed else Color.White,
                )
                ReelsActionButton(
                    onClick = onDownload,
                    icon = Icons.Filled.Download,
                    contentDescription = stringResource(R.string.download),
                )
                ReelsActionButton(
                    onClick = onSetWallpaper,
                    icon = Icons.Filled.Wallpaper,
                    contentDescription = stringResource(R.string.set_as_wallpaper),
                )
                ReelsActionButton(
                    onClick = onShare,
                    icon = Icons.Filled.Share,
                    contentDescription = stringResource(R.string.share),
                )
            }
        }

        // Bottom panel — metadata + tags. Collapsed by default showing the
        // metadata and a single truncated tag line ending in "...". Swipe up
        // to expand and reveal every tag chip; swipe down to collapse.
        // Fades away while zoomed, and sits above the bottom nav bar.
        AnimatedVisibility(
            visible = !isZoomed,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { panelHeight.value.toDp() })
                    .clip(RoundedCornerShape(topStart = KraftRadius.Large, topEnd = KraftRadius.Large))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .pointerInput(wallpaper.id, collapsedHeightPx, maxPanelHeightPx) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                // Freeze the settled height so the offset is relative
                                // to it; cancel any in-flight settle animation.
                                scope.launch { panelHeight.stop() }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                // dragAmount is negative when dragging up; subtract so
                                // pulling up grows the panel.
                                dragOffsetPx -= dragAmount
                            },
                            onDragEnd = {
                                val midpoint = (collapsedHeightPx + maxPanelHeightPx) / 2f
                                val currentPx = (panelHeight.value + dragOffsetPx)
                                    .coerceIn(collapsedHeightPx, maxPanelHeightPx)
                                expanded = currentPx > midpoint
                                scope.launch {
                                    panelHeight.animateTo(
                                        targetValue = if (expanded) maxPanelHeightPx else collapsedHeightPx,
                                        animationSpec = tween(300),
                                    )
                                }
                                dragOffsetPx = 0f
                            },
                        )
                    },
            ) {
                // Crossfade between the collapsed preview and the full chip list so
                // the content switch doesn't pop while the height animates.
                Crossfade(
                    targetState = expanded,
                    animationSpec = tween(200),
                    modifier = Modifier.align(Alignment.TopStart),
                ) { isExpanded ->
                    DetailPanelContent(
                        wallpaper = wallpaper,
                        onTagClick = onTagClick,
                        clickable = true,
                        collapsed = !isExpanded,
                        bottomPadding = navBarPadding,
                    )
                }
            }
        }
    }
}

/** A circular, semi-transparent action button for the right-edge stack. */
@Composable
private fun ReelsActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    tint: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * A clickable tag chip — tapping it opens the tag-filtered grid. Uses the
 * accent blue treatment (tinted fill + border) so tags read as interactive.
 * Pass `clickable = false` for the invisible measurement pass.
 */
@Composable
private fun DetailTagChip(name: String, onClick: () -> Unit, clickable: Boolean = true) {
    val shape = RoundedCornerShape(KraftRadius.Small)
    Text(
        text = "#$name",
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        modifier = Modifier
            .clip(shape)
            .background(KraftColors.AccentBlue.copy(alpha = 0.25f))
            .border(
                border = BorderStroke(1.dp, KraftColors.AccentBlue.copy(alpha = 0.55f)),
                shape = shape,
            )
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = KraftSpacing.Spacing8, vertical = KraftSpacing.Spacing4),
    )
}

/** The two metadata lines shown at the top of the bottom panel. */
@Composable
private fun DetailPanelMetadata(wallpaper: Wallpaper) {
    Column {
        Text(
            text = "${wallpaper.resolution}  ·  ${wallpaper.fileSizeFormatted()}",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
        )
        Spacer(Modifier.height(KraftSpacing.Spacing4))
        Text(
            text = "${wallpaperCategoryLabel(wallpaper.category)}  ·  ${stringResource(R.string.favorites_format, wallpaper.favorites)}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f),
        )
    }
}

/**
 * The bottom-panel content: metadata + tags.
 *
 * `collapsed = true` shows the metadata plus a preview of the first few tag
 * chips with an inline "…" after them — the same chip design as the expanded
 * state, just truncated. `collapsed = false` shows every tag as a clickable
 * chip, flowing vertically (FlowRow, no horizontal scroll). `clickable =
 * false` disables chip taps for the invisible measurement pass.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailPanelContent(
    wallpaper: Wallpaper,
    onTagClick: (String) -> Unit,
    clickable: Boolean = true,
    collapsed: Boolean = false,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = KraftSpacing.Spacing16)
            .padding(top = KraftSpacing.Spacing12, bottom = KraftSpacing.Spacing16 + bottomPadding),
    ) {
        DetailPanelMetadata(wallpaper = wallpaper)
        if (wallpaper.tags.isNotEmpty()) {
            Spacer(Modifier.height(KraftSpacing.Spacing12))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
            ) {
                val visibleTags = if (collapsed) wallpaper.tags.take(3) else wallpaper.tags
                visibleTags.forEach { tag ->
                    DetailTagChip(name = tag.name, onClick = { onTagClick(tag.name) }, clickable = clickable)
                }
                if (collapsed && wallpaper.tags.size > visibleTags.size) {
                    Text(
                        text = "...",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(horizontal = KraftSpacing.Spacing4, vertical = KraftSpacing.Spacing4),
                    )
                }
            }
        }
    }
}