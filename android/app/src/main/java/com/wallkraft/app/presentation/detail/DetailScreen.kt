package com.wallkraft.app.presentation.detail

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut


import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.outlined.KeyboardArrowDown
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.wallkraft.app.AppContainer
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftColors
import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.presentation.components.ErrorState
import com.wallkraft.app.presentation.components.WallpaperCropDialog
import com.wallkraft.app.presentation.components.ZoomableImage
import com.wallkraft.app.util.WallpaperActions
import com.wallkraft.app.domain.model.WallpaperPosition
import com.wallkraft.app.util.formatCount
import com.wallkraft.app.util.toUserMessage
import com.wallkraft.app.util.wallpaperCategoryLabel
import java.io.File
import kotlinx.coroutines.launch
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreen(
    container: AppContainer,
    wallpaperId: String,
    onBack: () -> Unit,
    onTagClick: (String) -> Unit = {},
    onUploaderClick: (String) -> Unit = {},
    onZoomChanged: (Boolean) -> Unit = {},
    navBarPadding: androidx.compose.ui.unit.Dp = 0.dp,
    previewThumb: String = "",
    previewPath: String = "",
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
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
    val wallpaperSetFailedMsg = stringResource(R.string.wallpaper_set_failed)

    // Smooth background: animate from transparent → black over 220ms on enter,
    // synchronized with the shared element's bounds animation. The alpha is
    // read by DetailContent to color the background.
    val backgroundAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        backgroundAlpha.animateTo(1f, animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f))
    }

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
                    // Build the shared element modifier for the detail image so
                    // it participates in the container-transform transition.
                    val sharedElementModifier: Modifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            Modifier.sharedElement(
                                state = rememberSharedContentState(key = wallpaper.id),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ -> spring(dampingRatio = 0.7f, stiffness = 400f) },
                            )
                        }
                    } else {
                        Modifier
                    }

                    DetailContent(
                        container = container,
                        wallpaper = wallpaper,
                        isFavorite = wallpaper.id in uiState.favoriteIds,
                        isUploaderDeleted = uiState.isDetailLoaded && wallpaper.uploaderName.isBlank(),
                        dataSaverEnabled = dataSaverEnabled,
                        imageModel = container.favoriteImageStore.fileFor(wallpaper.id) ?: wallpaper.path,
                        backgroundAlpha = backgroundAlpha.value,
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
                                    container.favoriteImageStore.fileFor(wallpaper.id),
                                )
                            }
                        },
                        onBack = onBack,
                        onTagClick = onTagClick,
                        onUploaderClick = onUploaderClick,
                        onZoomChanged = onZoomChanged,
                        navBarPadding = navBarPadding,
                        modifier = Modifier.fillMaxSize(),
                        sharedElementModifier = sharedElementModifier,
                        animatedVisibilityScope = animatedVisibilityScope,
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
                container.favoriteImageStore.fileFor(setTarget.id),
            )
            resolving = false
        }
        val file = resolvedFile
        when {
            file != null -> WallpaperCropDialog(
                imageFile = file,
                onDismiss = { setWallpaperTarget = null },
                // The dialog owns the feedback: it shows a spinner while the
                // wallpaper applies, a centered checkmark on success (then
                // dismisses itself), or a snackbar on failure (and stays open).
                // We just apply the wallpaper and report whether it worked.
                onConfirm = { cropped, position ->
                    val ok = WallpaperActions.setAsWallpaper(context, cropped, position)
                    if (ok) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    ok
                },
            )
            resolving -> {
                // Still resolving the image (downloading a non-favorite's
                // full-res into cache). Show a spinner so the tap isn't a
                // silent no-op while the network does its thing.
                Dialog(
                    onDismissRequest = { setWallpaperTarget = null },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false,
                    ),
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
    container: AppContainer,
    wallpaper: Wallpaper,
    isFavorite: Boolean,
    isUploaderDeleted: Boolean,
    dataSaverEnabled: Boolean?,
    imageModel: Any,
    backgroundAlpha: Float = 1f,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    onSetWallpaper: () -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit,
    onTagClick: (String) -> Unit,
    onUploaderClick: (String) -> Unit,
    onZoomChanged: (Boolean) -> Unit,
    navBarPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    sharedElementModifier: Modifier = Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val context = LocalContext.current
    var isZoomed by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }
    val contentScope = rememberCoroutineScope()
    val hapticLocal = LocalHapticFeedback.current

    // Unified back while zoomed: animate internal zoom → fit and shared-element
    // bounds viewport→tile together in a single 220ms motion. Without this the
    // exit is two-step: snap to fit then shrink to tile.
    var resetZoomSignal by remember { mutableIntStateOf(0) }
    fun handleBack() {
        if (isZoomed) {
            resetZoomSignal++
            contentScope.launch {
                // One frame so ZoomableImage's LaunchedEffect(resetZoomSignal)
                // starts its 220ms animateTo before we pop — both run concurrently.
                kotlinx.coroutines.delay(16)
                onBack()
            }
        } else {
            onBack()
        }
    }
    BackHandler(enabled = isZoomed) { handleBack() }

    // Chrome alpha — fades in from 0→1 on entry using the same spring spec
    // as the shared element (dampingRatio=0.7, stiffness=400). Both animations
    // start at the same frame with the same curve, so they feel like one
    // cohesive transition.
    var chromeVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { chromeVisible = true }
    val chromeAlpha by animateFloatAsState(
        targetValue = if (chromeVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "chromeAlpha",
    )

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

    BoxWithConstraints(modifier = modifier.background(Color.Black.copy(alpha = backgroundAlpha))) {
        val constraintsMaxHeight = maxHeight
        val viewW = maxWidth.value
        val viewH = maxHeight.value
        // Fit size at 1x: ContentScale.Fit inside the viewport.
        val displayedW = if (aspect <= 0f) viewW else min(viewW, viewH * aspect)
        val displayedH = if (aspect <= 0f) viewH else min(viewW / aspect, viewH)
        // Fill scale (relative to 1x fit) where the image covers the entire
        // viewport — no black bars. Narrow fills height, wide fills width.
        val fillRelative = if (displayedW <= 0f || displayedH <= 0f) 1f
            else maxOf(viewW / displayedW, viewH / displayedH).coerceIn(1f, 8f)
        // 1:1 native resolution (pixel zoom), always deeper than fill.
        val nativeRelative = if (aspect <= 0f || displayedH <= 0f) {
            (fillRelative * 2f).coerceAtMost(8f)
        } else {
            (h / displayedH).coerceIn(fillRelative * 1.2f, 8f)
        }
        // Double-tap cycle: fit (1x, full image) -> fill (no bars) -> native -> fit
        val zoomLevels = remember(fillRelative, nativeRelative) {
            listOf(fillRelative, nativeRelative, 1f)
        }

        // -- No hero transition: the detail opens instantly showing the
        // thumbnail, and the full-res image loads right away behind a subtle
        // top loading bar. Simple and predictable — no animation to get wrong.
        var fullResLoaded by remember { mutableStateOf(false) }
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

        ZoomableImage(
            model = imageModel,
            placeholderModel = wallpaper.thumbnail,
            contentDescription = wallpaper.resolution,
            zoomLevels = zoomLevels,
            imageWidth = wallpaper.dimensionX,
            imageHeight = wallpaper.dimensionY,
            resetZoomSignal = resetZoomSignal,
            clipRadius = KraftRadius.Standard * (1f - backgroundAlpha),
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
            },
            loadFullRes = fullResRequested,
            modifier = Modifier.fillMaxSize(),
            sharedElementModifier = sharedElementModifier,
        )

        // Subtle loading bar at the very top while the full-res image loads.
        // Indeterminate: pulses across the top to signal activity without
        // faking progress. Only shown when the full-res is actually being
        // requested — in data saver mode that's once the user zooms.
        AnimatedVisibility(
            visible = fullResRequested && !fullResLoaded && !isZoomed,
            enter = fadeIn(animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)),
            exit = fadeOut(animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)),
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
        ) {
            LinearProgressIndicator(
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
            enter = fadeIn(animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)),
            exit = fadeOut(animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)),
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

        // ── Chrome overlay — all UI elements in a single graphicsLayer ──
        // The shared element transition uses its own graphicsLayer compositing
        // layer which bypasses zIndex. Wrapping ALL chrome in a single Box
        // with graphicsLayer forces it into a separate, higher compositing
        // layer that's always rendered on top of the image.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = chromeAlpha }
        ) {
            // Top bar — back button with gradient.
            if (!isZoomed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.55f),
                                    Color.Transparent,
                                ),
                            ),
                        )
                        .statusBarsPadding(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = KraftSpacing.Spacing12, vertical = KraftSpacing.Spacing12),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable(onClick = { handleBack() }),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            // Invisible measurement pass for collapsed panel height.
            var collapsedContentHeight by remember { mutableIntStateOf(0) }
            var fullContentHeight by remember { mutableIntStateOf(0) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(0f)
                    .onSizeChanged { collapsedContentHeight = it.height },
            ) {
                DetailPanelContent(
                    wallpaper = wallpaper,
                    onTagClick = onTagClick,
                    onUploaderClick = onUploaderClick,
                    isUploaderDeleted = isUploaderDeleted,
                    clickable = false,
                    collapsed = true,
                    loadAvatar = false,
                    bottomPadding = navBarPadding,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = constraintsMaxHeight)
                    .alpha(0f)
                    .onSizeChanged { fullContentHeight = it.height },
            ) {
                DetailPanelContent(
                    wallpaper = wallpaper,
                    onTagClick = onTagClick,
                    onUploaderClick = onUploaderClick,
                    isUploaderDeleted = isUploaderDeleted,
                    clickable = false,
                    collapsed = false,
                    loadAvatar = false,
                    bottomPadding = navBarPadding,
                )
            }

            val density = LocalDensity.current
            val navInsetPx = remember {
                (context as? Activity)?.window?.decorView
                    ?.let { ViewCompat.getRootWindowInsets(it) }
                    ?.getInsets(WindowInsetsCompat.Type.navigationBars())
                    ?.bottom ?: 0
            }
            val bottomInsetPx = with(density) {
                KraftSpacing.Spacing16.toPx() + navBarPadding.toPx() + navInsetPx
            }
            val maxPanelHeightPx = min(
                fullContentHeight.toFloat(),
                with(density) { (constraintsMaxHeight * 0.65f).toPx() },
            )
            val gestureBarHeight = with(density) { navBarPadding.toPx() }
            val collapsedHeightPx = min(
                (collapsedContentHeight.toFloat() - bottomInsetPx + gestureBarHeight).coerceAtLeast(0f),
                maxPanelHeightPx,
            )
            val contentOverflows = fullContentHeight.toFloat() > maxPanelHeightPx

            var expanded by remember(wallpaper.id) { mutableStateOf(false) }

            // ── Action buttons (horizontal row above bottom panel) ──────
            if (!isZoomed && !expanded) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = navBarPadding + with(density) { (collapsedHeightPx + 8.dp.toPx()).toDp() })
                        .padding(horizontal = KraftSpacing.Spacing16),
                ) {
                    DetailTextButton(
                        text = stringResource(R.string.set_as_wallpaper),
                        onClick = onSetWallpaper,
                        modifier = Modifier.weight(1f),
                    )
                    DetailCircleButton(
                        onClick = onToggleFavorite,
                        icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites),
                        iconTint = if (isFavorite) KraftColors.AccentRed else Color.White,
                    )
                    DetailCircleButton(
                        onClick = onDownload,
                        icon = Icons.Filled.Download,
                        contentDescription = stringResource(R.string.download),
                    )
                    if (isSharing) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        DetailCircleButton(
                            onClick = {
                                hapticLocal.performHapticFeedback(HapticFeedbackType.LongPress)
                                isSharing = true
                                contentScope.launch {
                                    try {
                                        WallpaperActions.share(
                                            context,
                                            wallpaper,
                                            container.favoriteImageStore.fileFor(wallpaper.id),
                                        )
                                    } finally {
                                        isSharing = false
                                    }
                                }
                            },
                            icon = Icons.Filled.Share,
                            contentDescription = stringResource(R.string.share),
                        )
                    }
                }
            }

            // Bottom panel
            BottomPanel(
                wallpaper = wallpaper,
                onTagClick = onTagClick,
                onUploaderClick = onUploaderClick,
                isUploaderDeleted = isUploaderDeleted,
                collapsedHeightPx = collapsedHeightPx,
                maxPanelHeightPx = maxPanelHeightPx,
                contentOverflows = contentOverflows,
                navBarPadding = navBarPadding,
                isZoomed = isZoomed,
                expanded = expanded,
                onExpandedChange = { expanded = it },
                chromeAlpha = 1f, // parent already handles alpha
            )
        }
    }
}

/**
 * The bottom panel: drag handle, uploader row, stat pills, and tags.
 *
 * The panel always renders its full content and clips it to its current
 * height, so dragging reveals the extra content progressively with zero
 * content switching — the only thing that changes while dragging is the Box
 * height. All drag state lives here so per-frame drag updates recompose only
 * this composable, never the whole screen (which is what made the panel
 * stutter before).
 *
 * `expanded` is owned by the caller (the right-edge stack reads it to fade
 * out); this composable only reports it via [onExpandedChange] as the drag
 * crosses the threshold or settles past the expand threshold.
 */
@Composable
private fun BottomPanel(
    wallpaper: Wallpaper,
    onTagClick: (String) -> Unit,
    onUploaderClick: (String) -> Unit,
    isUploaderDeleted: Boolean,
    collapsedHeightPx: Float,
    maxPanelHeightPx: Float,
    contentOverflows: Boolean,
    navBarPadding: androidx.compose.ui.unit.Dp,
    isZoomed: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    chromeAlpha: Float = 1f,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    // Settled height, animated between collapsed and expanded.
    val panelHeight = remember(wallpaper.id) { Animatable(0f) }
    // Drag offset in px, tracked synchronously so the release decision is
    // correct even though the Animatable settles asynchronously.
    var dragOffsetPx by remember(wallpaper.id) { mutableFloatStateOf(0f) }
    // Once the user has touched the panel we stop auto-snapping it to the
    // measured heights — the panel is theirs from then on.
    var userInteracted by remember(wallpaper.id) { mutableStateOf(false) }
    // Release velocity in px/s (upward positive), smoothed from the last drag
    // events so a quick flick settles the panel even when the pull distance
    // is small. Without this, expanding a tall panel (many tags) would need
    // a long drag because the settle threshold scales with content height.
    var dragVelocityPxPerSec by remember(wallpaper.id) { mutableFloatStateOf(0f) }
    var lastDragTimestamp by remember(wallpaper.id) { mutableLongStateOf(0L) }

    // Settle the panel to the target height. Runs on first layout and again
    // whenever the measured heights change (e.g. the real detail loads and
    // the uploader row appears). Once the user has dragged the panel it's
    // theirs — except when the content grows past the current height, so
    // the panel never clips its own content. `expanded` is deliberately NOT
    // a key here: onDragEnd owns the settle, and keying on it would let
    // this effect race the finger every time the drag threshold flips it.
    LaunchedEffect(collapsedHeightPx, maxPanelHeightPx, wallpaper.id) {
        if (collapsedHeightPx > 0f) {
            val target = if (expanded) maxPanelHeightPx else collapsedHeightPx
            if (!userInteracted || target > panelHeight.value) {
                panelHeight.animateTo(
                    targetValue = target,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                )
            }
        }
    }

    // The panel is always anchored to the bottom bar: its height follows the
    // finger while dragging, clamped to the collapsed/expanded range, and it
    // grows upward from the bottom bar as it expands.
    val currentHeightPx = (panelHeight.value + dragOffsetPx)
        .coerceIn(collapsedHeightPx, maxPanelHeightPx)

    // Wrapper Box restores the BoxScope so the panel can align to the bottom
    // of the content area (BottomPanel is a standalone composable, not a
    // BoxScope receiver).
    Box(modifier = Modifier.fillMaxSize()) {
        // AnimatedVisibility wraps its content in an internal Box that
        // center-aligns oversized children. Because the inner measurement
        // Box (requiredHeight = maxPanel) is taller than the outer clip Box
        // (currentHeight), AnimatedVisibility's internal Box pushes the
        // entire panel upward by ~257px, hiding the handle/uploader/hint.
        // Using graphicsLayer alpha bypasses that internal layout entirely.
        // Uses chromeAlpha (synced to shared element) instead of separate animation.

        // The outer panel is a custom Layout (not a Box) because Box
        // center-aligns children that are taller than the Box, even with
        // contentAlignment = TopStart. This pushes the handle/uploader/hint
        // above the visible clip region when collapsed. A custom Layout lets
        // us measure content at maxPanelHeight (so every child lays out at
        // its natural size) while always placing it at y=0. The clip +
        // background + pointerInput work on the Layout's reported height
        // (currentHeightPx), which is what drives the panel's visual size.
        val maxH = maxPanelHeightPx.toInt()
        Layout(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .zIndex(2f)
                .graphicsLayer { alpha = chromeAlpha * (if (isZoomed) 0f else 1f) }
                .height(with(density) { currentHeightPx.toDp() })
                .clip(RoundedCornerShape(topStart = KraftRadius.Large, topEnd = KraftRadius.Large))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .pointerInput(wallpaper.id, collapsedHeightPx, maxPanelHeightPx) {
                    // Settled height where the drag started. Read at release to
                    // tell an upward pull from a downward drag, so the expand and
                    // collapse thresholds can be asymmetric.
                    var dragStartPx = 0f
                    detectVerticalDragGestures(
                        onDragStart = {
                            // Freeze the settled height so the offset is relative
                            // to it; cancel any in-flight settle animation. From
                            // here on the panel is user-controlled.
                            userInteracted = true
                            dragVelocityPxPerSec = 0f
                            lastDragTimestamp = 0L
                            dragStartPx = panelHeight.value
                            scope.launch { panelHeight.stop() }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            // Track release velocity from the event timestamps.
                            // dragAmount is negative when dragging up; negate so
                            // upward is positive.
                            val now = change.uptimeMillis
                            if (lastDragTimestamp > 0L) {
                                val dt = (now - lastDragTimestamp).coerceAtLeast(1L)
                                val instantVelocity = -dragAmount / dt * 1000f
                                dragVelocityPxPerSec =
                                    dragVelocityPxPerSec * 0.7f + instantVelocity * 0.3f
                            }
                            lastDragTimestamp = now
                            // dragAmount is negative when dragging up; subtract so
                            // pulling up grows the panel.
                            dragOffsetPx -= dragAmount
                            // Flip state live while dragging: once the panel
                            // clears the collapsed bar by a small margin the
                            // right-edge stack fades out. The panel Box's height
                            // follows the finger, so its clip reveals the extra
                            // content progressively — no content switching.
                            onExpandedChange(
                                (panelHeight.value + dragOffsetPx) >
                                    collapsedHeightPx + with(density) { 8.dp.toPx() },
                            )
                        },
                        onDragEnd = {
                            val currentPx = (panelHeight.value + dragOffsetPx)
                                .coerceIn(collapsedHeightPx, maxPanelHeightPx)
                            val rangePx = maxPanelHeightPx - collapsedHeightPx
                            // Expand threshold: a small, consistent pull opens the
                            // panel regardless of content height. Capped at half
                            // the range so it always sits below the expanded
                            // height for very short content.
                            val expandThresholdPx = collapsedHeightPx +
                                min(with(density) { 40.dp.toPx() }, rangePx * 0.5f)
                            // Collapse needs a deliberate drag past the midpoint
                            // (or a downward flick) — a small downward drag on a
                            // tall panel shouldn't close it.
                            val midpointPx = (collapsedHeightPx + maxPanelHeightPx) / 2f
                            // A quick flick settles by velocity; a slow drag
                            // settles by position. Asymmetric thresholds: a small
                            // pull up opens the panel regardless of content
                            // height, while collapsing needs a deliberate drag
                            // down past the midpoint.
                            val velocityThresholdPx = with(density) { 380.dp.toPx() }
                            val draggedUp = currentPx > dragStartPx
                            val settleExpanded = when {
                                dragVelocityPxPerSec > velocityThresholdPx -> true
                                dragVelocityPxPerSec < -velocityThresholdPx -> false
                                draggedUp -> currentPx > expandThresholdPx
                                else -> currentPx > midpointPx
                            }
                            onExpandedChange(settleExpanded)
                            scope.launch {
                                // Snap to where the finger released, then
                                // animate to the target — no jump back.
                                // 220ms matches shared-element so panel settles
                                // in sync with chrome/background.
                                panelHeight.snapTo(currentPx)
                                panelHeight.animateTo(
                                    targetValue = if (settleExpanded) maxPanelHeightPx else collapsedHeightPx,
                                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                                )
                            }
                            dragOffsetPx = 0f
                        },
                    )
                },
            content = {
                DetailPanelContent(
                    wallpaper = wallpaper,
                    onTagClick = onTagClick,
                    onUploaderClick = onUploaderClick,
                    isUploaderDeleted = isUploaderDeleted,
                    clickable = true,
                    collapsed = !expanded,
                    tagsScrollable = contentOverflows,
                    pullHintVisible = !expanded,
                    bottomPadding = navBarPadding,
                )
            },
            measurePolicy = { measurables: List<androidx.compose.ui.layout.Measurable>, constraints: Constraints ->
                val childConstraints = Constraints(
                    minWidth = constraints.minWidth,
                    maxWidth = constraints.maxWidth,
                    minHeight = maxH,
                    maxHeight = maxH,
                )
                val placeable = measurables.first().measure(childConstraints)
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(0, 0)
                }
            },
        )
    }
}

/** Circular action button for the detail screen — 44dp, solid background. */
@Composable
private fun DetailCircleButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Pill-shaped text button for "Set as Wallpaper" — matches the filter bar style. */
@Composable
private fun DetailTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
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

/** The uploader row's three visual states, crossfaded as the detail loads. */
private enum class UploaderState { Loading, Loaded, Deleted }

/**
 * The bottom-panel content: drag handle, uploader row, stat pills, and tags.
 *
 * `collapsed = true` shows just the drag handle and uploader row — the compact
 * bar. `collapsed = false` adds the divider, stat pills, and all tag chips.
 * `clickable = false` disables chip taps for the invisible measurement pass.
 * `loadAvatar = false` skips the uploader avatar network fetch in that same
 * pass (the fixed-size placeholder keeps the measured height identical).
 * `tagsScrollable` attaches a vertical scroll to the tag chips — only used
 * when the expanded content would overflow the panel's max height.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailPanelContent(
    wallpaper: Wallpaper,
    onTagClick: (String) -> Unit,
    onUploaderClick: (String) -> Unit,
    isUploaderDeleted: Boolean,
    clickable: Boolean = true,
    collapsed: Boolean = false,
    loadAvatar: Boolean = true,
    tagsScrollable: Boolean = false,
    pullHintVisible: Boolean = true,
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
        // Drag handle — 36×5, 0.3 alpha + soft shadow for legibility.
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 36.dp, height = 5.dp)
                .shadow(4.dp, RoundedCornerShape(2.5.dp), clip = false)
                .clip(RoundedCornerShape(2.5.dp))
                .background(Color.White.copy(alpha = 0.3f)),
        )
        Spacer(Modifier.height(KraftSpacing.Spacing16))

        // Uploader row. While the preview is still loading (no uploader data yet) a
        // pulsing skeleton occupies the slot; once the real detail loads it
        // crossfades to the uploader row (or the account-deleted state). All
        // three states are the same height, so the panel never changes size —
        // the load reads as content arriving, not a layout jump. This is the
        // collapsed bar's content — the divider, stats, and tags only appear
        // expanded.
        val uploaderState = when {
            wallpaper.uploaderName.isNotBlank() -> UploaderState.Loaded
            isUploaderDeleted -> UploaderState.Deleted
            else -> UploaderState.Loading
        }
        Crossfade(
            targetState = uploaderState,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
            label = "uploaderRow",
            modifier = Modifier,
        ) { state ->
            when (state) {
                UploaderState.Loaded -> UploaderRow(
                    name = wallpaper.uploaderName,
                    avatarUrl = wallpaper.uploaderAvatarUrl,
                    createdAt = wallpaper.createdAt,
                    loadAvatar = loadAvatar,
                    onClick = { onUploaderClick(wallpaper.uploaderName) },
                    clickable = clickable,
                )
                UploaderState.Deleted -> DeletedUploaderRow()
                UploaderState.Loading -> UploaderRowPlaceholder()
            }
        }

        // Pull hint — "More details ⌄" — tells first-time users the bar swipes
        // up. It's placed between the uploader and the divider and rendered in
        // both collapsed and expanded states, so the collapsed panel's measured
        // height includes it and it's visible without any drag. It grows/shrinks
        // in place so the divider and stats below slide smoothly, and fades
        // away entirely once the user starts dragging (expanded) — it would be
        // pointless while the panel is visibly moving.
        Spacer(Modifier.height(KraftSpacing.Spacing8))
        AnimatedVisibility(
            visible = pullHintVisible,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)),
            modifier = Modifier,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.more_details),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.55f),
                        letterSpacing = 0.4.sp,
                    ),
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.size(12.dp),
                )
            }
        }
        Spacer(Modifier.height(KraftSpacing.Spacing8))

        // Expanded content: stat pills and tags. The live panel always renders
        // this (clipped to its current height), so the reveal is purely the
        // Box clip following the finger — no switching, no fade.
        if (!collapsed) {
            Column {
                Spacer(Modifier.height(KraftSpacing.Spacing16))

                // Stat pills: resolution, file size, views, favorites — category
                // is implied by tags and adds no decision value.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                ) {
                    StatPill(wallpaper.resolution)
                    StatPill(wallpaper.fileSizeFormatted())
                    if (wallpaper.views > 0) {
                        StatPill("${formatCount(wallpaper.views)} views")
                    }
                    if (wallpaper.favorites > 0) {
                        StatPill("${formatCount(wallpaper.favorites)} favorites")
                    }
                }

                if (wallpaper.tags.isNotEmpty()) {
                    Spacer(Modifier.height(KraftSpacing.Spacing16))
                    Text(
                        text = stringResource(R.string.tags_heading),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.55f),
                            letterSpacing = 0.4.sp,
                        ),
                    )
                    Spacer(Modifier.height(KraftSpacing.Spacing8))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                        verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                        modifier = if (tagsScrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier,
                    ) {
                        wallpaper.tags.forEach { tag ->
                            DetailTagChip(name = tag.name, onClick = { onTagClick(tag.name) }, clickable = clickable)
                        }
                    }
                }
            }
        }
    }
}

/** A compact stat pill (resolution, size, category) in the bottom panel. */
@Composable
private fun StatPill(text: String) {
    val shape = RoundedCornerShape(KraftRadius.Small)
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(color = Color.White),
        modifier = Modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), shape)
            .padding(horizontal = KraftSpacing.Spacing8, vertical = KraftSpacing.Spacing4),
    )
}

/**
 * The clickable uploader row: avatar + username. Tapping it opens the
 * uploader's wallpapers (search query `@username`), mirroring tag behavior.
 * Only the avatar + name are clickable — the row wraps its content instead of
 * filling the panel width, so the tap target (and its ripple) stays compact
 * rather than covering the whole strip.
 */
@Composable
private fun UploaderRow(
    name: String,
    avatarUrl: String,
    createdAt: String = "",
    loadAvatar: Boolean,
    onClick: () -> Unit,
    clickable: Boolean,
) {
    val viewByDesc = stringResource(R.string.view_by_uploader, name)
    val timeAgo = remember(createdAt) { relativeTime(createdAt) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = viewByDesc }
            .padding(vertical = KraftSpacing.Spacing2),
    ) {
        UploaderAvatar(avatarUrl = avatarUrl, name = name, loadAvatar = loadAvatar)
        Spacer(Modifier.width(KraftSpacing.Spacing12))
        Text(
            text = if (timeAgo != null) "$name • $timeAgo" else name,
            style = MaterialTheme.typography.titleSmall.copy(
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // Cap the width so a very long username ellipsizes instead of
            // overflowing the panel; the tap target stays compact.
            modifier = Modifier.widthIn(max = 260.dp),
        )
    }
}

/**
 * A pulsing skeleton shown in the uploader slot while the real uploader
 * details load. Same dimensions as the real row (32dp avatar + name), so the
 * panel never changes size when the data arrives — it reads as content
 * loading, not a layout jump.
 */
@Composable
private fun UploaderRowPlaceholder() {
    val pulse by rememberInfiniteTransition(label = "uploaderPlaceholder").animateFloat(
        initialValue = 0.3f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "uploaderPlaceholderAlpha",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = KraftSpacing.Spacing2),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = pulse)),
        )
        Spacer(Modifier.width(KraftSpacing.Spacing12))
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(KraftRadius.Small))
                .background(Color.White.copy(alpha = pulse)),
        )
    }
}

/**
 * The uploader's avatar: the real image when available, otherwise the
 * username's initial on a deterministic accent color. `loadAvatar = false`
 * (measurement pass) renders the placeholder directly — same size, no network.
 */
@Composable
private fun UploaderAvatar(
    avatarUrl: String,
    name: String,
    loadAvatar: Boolean,
    modifier: Modifier = Modifier,
) {
    val initial = name.firstOrNull()?.uppercase() ?: "?"
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(avatarColor(name)),
    ) {
        if (loadAvatar && avatarUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { InitialLetter(initial) },
                error = { InitialLetter(initial) },
            )
        } else {
            InitialLetter(initial)
        }
    }
}

@Composable
private fun InitialLetter(initial: String) {
    Text(
        text = initial,
        style = MaterialTheme.typography.labelMedium.copy(
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        ),
    )
}

/**
 * Shown when the loaded detail has no uploader — the account was deleted but
 * the wallpaper remains. Not clickable: there is no user to browse.
 */
@Composable
private fun DeletedUploaderRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = KraftSpacing.Spacing2),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)),
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(KraftSpacing.Spacing12))
        Text(
            text = stringResource(R.string.account_deleted),
            style = MaterialTheme.typography.titleSmall.copy(
                color = Color.White.copy(alpha = 0.5f),
            ),
        )
    }
}

/** Deterministic avatar color per username, picked from the Aurora palette. */
private val avatarPalette = listOf(
    KraftColors.AccentBlue,
    KraftColors.AccentBlueDark,
    KraftColors.AccentOrange,
    KraftColors.AccentGreen,
    KraftColors.AccentRed,
)

private fun avatarColor(name: String): Color =
    avatarPalette[(name.hashCode() and Int.MAX_VALUE) % avatarPalette.size]

/** Parses Wallhaven `created_at` (yyyy-MM-dd HH:mm:ss UTC) to a short relative time. */
private fun relativeTime(createdAt: String): String? {
    if (createdAt.isBlank()) return null
    return try {
        val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val ldt = java.time.LocalDateTime.parse(createdAt, fmt)
        val instant = ldt.atZone(java.time.ZoneId.of("UTC")).toInstant()
        val now = java.time.Instant.now()
        val d = java.time.Duration.between(instant, now)
        if (d.isNegative) return null
        val mins = d.toMinutes()
        val hours = d.toHours()
        val days = d.toDays()
        when {
            mins < 1 -> "just now"
            mins < 60 -> "${mins}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            days < 30 -> "${days / 7}w ago"
            days < 365 -> "${days / 30}mo ago"
            else -> "${days / 365}y ago"
        }
    } catch (_: Exception) {
        null
    }
}