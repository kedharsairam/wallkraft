package com.wallkraft.app.presentation.detail

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.wallkraft.app.AppContainer
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftColors
import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.core.design.KraftIconSize
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.presentation.components.ZoomableImage
import com.wallkraft.app.util.WallpaperActions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.min

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
 *
 * Layout: the image lives inside BoxWithConstraints (needed for zoom-level
 * calculations and invisible measurement passes). The chrome overlay lives
 * OUTSIDE BoxWithConstraints as a later sibling in the parent Box — this
 * ensures it renders above the shared element's compositing layer via source
 * order, without needing zIndex.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun DetailContent(
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
    onBack: () -> Unit,
    onTagClick: (String) -> Unit,
    onUploaderClick: (String) -> Unit,
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
    DisposableEffect(Unit) {
        onDispose { isSharing = false }
    }

    var resetZoomSignal by remember { mutableIntStateOf(0) }
    fun handleBack() {
        if (isZoomed) {
            resetZoomSignal++
            contentScope.launch {
                delay(16)
                onBack()
            }
        } else {
            onBack()
        }
    }
    androidx.activity.compose.BackHandler(enabled = isZoomed) { handleBack() }

    var chromeVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { chromeVisible = true }
    val chromeAlpha by animateFloatAsState(
        targetValue = if (chromeVisible) 1f else 0f,
        animationSpec = SharedElementSpringFloat,
        label = "chromeAlpha",
    )

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
    val ht = wallpaper.dimensionY.toFloat().takeIf { it > 0f } ?: 1f
    val aspect = w / ht

    // ── Panel measurement state (hoisted so chrome can read outside BoxWithConstraints) ──
    var collapsedContentHeight by remember { mutableIntStateOf(0) }
    var fullContentHeight by remember { mutableIntStateOf(0) }
    var constraintsMaxHeightDp by remember { mutableStateOf(0.dp) }
    var expanded by remember(wallpaper.id) { mutableStateOf(false) }

    // ── Image + invisible measurement boxes (inside BoxWithConstraints) ──
    BoxWithConstraints(modifier = modifier.background(Color.Black.copy(alpha = backgroundAlpha))) {
        constraintsMaxHeightDp = maxHeight
        val viewW = maxWidth.value
        val viewH = maxHeight.value
        val displayedW = if (aspect <= 0f) viewW else min(viewW, viewH * aspect)
        val displayedH = if (aspect <= 0f) viewH else min(viewW / aspect, viewH)
        val fillRelative = if (displayedW <= 0f || displayedH <= 0f) 1f
            else maxOf(viewW / displayedW, viewH / displayedH).coerceIn(1f, 8f)
        val nativeRelative = if (aspect <= 0f || displayedH <= 0f) {
            (fillRelative * 2f).coerceAtMost(8f)
        } else {
            (ht / displayedH).coerceIn(fillRelative * 1.2f, 8f)
        }
        val zoomLevels = remember(fillRelative, nativeRelative) {
            listOf(fillRelative, nativeRelative, 1f)
        }

        var fullResLoaded by remember { mutableStateOf(false) }
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
            onLoaded = { fullResLoaded = true },
            onZoomChanged = { scale -> isZoomed = scale > 1.01f },
            loadFullRes = fullResRequested,
            modifier = Modifier.fillMaxSize(),
            sharedElementModifier = sharedElementModifier,
        )

        AnimatedVisibility(
            visible = fullResRequested && !fullResLoaded && !isZoomed,
            enter = androidx.compose.animation.fadeIn(animationSpec = SharedElementSpringFloat),
            exit = androidx.compose.animation.fadeOut(animationSpec = SharedElementSpringFloat),
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
        ) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(KraftSpacing.ProgressBarHeight),
                color = KraftColors.AccentGreen,
                trackColor = Color.Transparent,
            )
        }

        AnimatedVisibility(
            visible = isZoomed && fullResRequested && !fullResLoaded,
            enter = androidx.compose.animation.fadeIn(animationSpec = SharedElementSpringFloat),
            exit = androidx.compose.animation.fadeOut(animationSpec = SharedElementSpringFloat),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = KraftSpacing.Spacing24),
        ) {
            Surface(
                shape = RoundedCornerShape(KraftRadius.Small),
                color = Color.Black.copy(alpha = KraftConstants.OverlayPillAlpha),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = KraftSpacing.Spacing12, vertical = KraftSpacing.Spacing8),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(KraftIconSize.Small),
                        strokeWidth = KraftSpacing.SpinnerStroke,
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

        // Invisible measurement boxes — these MUST stay inside BoxWithConstraints
        // because they need the viewport constraints to measure correctly.
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
                .heightIn(max = maxHeight)
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
    }
    // ── End BoxWithConstraints ──

    // ── Chrome overlay (OUTSIDE BoxWithConstraints) ──
    // Placed after BoxWithConstraints in the outer Box, so source order
    // ensures it renders above the shared element's compositing layer.
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
    val constraintsMaxHeight = constraintsMaxHeightDp
    val maxPanelHeightPx = min(
        fullContentHeight.toFloat(),
        with(density) { (constraintsMaxHeight * 0.65f).toPx() },
    )
    val gestureBarHeight = with(density) { navBarPadding.toPx() }
    // Small upward offset so the collapsed panel doesn't sit too low
    // against the gesture bar. Matches the visual centering of the
    // action buttons above it.
    val panelLiftPx = with(density) { 4.dp.toPx() }
    val collapsedHeightPx = min(
        (collapsedContentHeight.toFloat() - bottomInsetPx + gestureBarHeight - panelLiftPx).coerceAtLeast(0f),
        maxPanelHeightPx,
    )
    val contentOverflows = fullContentHeight.toFloat() > maxPanelHeightPx

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = chromeAlpha }
    ) {
        // Top bar
        if (!isZoomed) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = KraftConstants.OverlayScrimAlpha),
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
                            .size(KraftSpacing.TouchTarget)
                            .clip(CircleShape)
                            .background(KraftColors.GlassDark)
                            .border(KraftSpacing.BorderWidth, KraftColors.GlassBorderDark, CircleShape)
                            .clickable(onClick = {
                                hapticLocal.performHapticFeedback(HapticFeedbackType.LongPress)
                                handleBack()
                            }),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White,
                            modifier = Modifier.size(KraftIconSize.Medium),
                        )
                    }
                }
            }
        }

        // Action buttons
        if (!isZoomed && !expanded) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = navBarPadding + with(density) { (collapsedHeightPx + KraftSpacing.Spacing12.toPx()).toDp() })
                    .padding(horizontal = KraftSpacing.Spacing16),
            ) {
                DetailCircleButton(
                    onClick = {
                        hapticLocal.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSetWallpaper()
                    },
                    icon = Icons.Filled.Wallpaper,
                    contentDescription = stringResource(R.string.set_as_wallpaper),
                    text = stringResource(R.string.set_as_wallpaper),
                    borderColor = KraftColors.AuroraBlue,
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
                            .size(KraftSpacing.TouchTarget)
                            .clip(CircleShape)
                            .background(KraftColors.GlassDark)
                            .border(KraftSpacing.BorderWidth, KraftColors.GlassBorderDark, CircleShape),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(KraftIconSize.Medium),
                            strokeWidth = KraftSpacing.SpinnerStroke,
                            color = Color.White,
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
            onExpandedChange = { newExpanded ->
                if (newExpanded != expanded) {
                    hapticLocal.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                expanded = newExpanded
            },
            chromeAlpha = 1f,
        )
    }
}
