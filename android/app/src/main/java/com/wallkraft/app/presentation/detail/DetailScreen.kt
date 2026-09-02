package com.wallkraft.app.presentation.detail

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wallkraft.app.AppContainer
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftColors
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.presentation.components.ErrorState
import com.wallkraft.app.presentation.components.WallpaperCropDialog
import com.wallkraft.app.util.WallpaperActions
import com.wallkraft.app.util.toUserMessage
import kotlinx.coroutines.launch
import java.io.File

internal val SharedElementSpring = spring<androidx.compose.ui.geometry.Rect>(dampingRatio = 0.7f, stiffness = 400f)
internal val SharedElementSpringFloat = spring<Float>(dampingRatio = 0.7f, stiffness = 400f)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreen(
    container: AppContainer,
    wallpaperId: String,
    onBack: () -> Unit,
    onTagClick: (String) -> Unit = {},
    onUploaderClick: (String) -> Unit = {},
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
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val wallpaper = uiState.wallpaper
    var setWallpaperTarget by remember { mutableStateOf<Wallpaper?>(null) }

    // Data saver: when enabled, the full-res image is deferred until the user
    // zooms (or the image is already local -- favorites cost zero data). The
    // thumbnail still renders instantly, so the screen never feels slow.
    // Read once (null until known) so the full-res decision is never made
    // against the default value -- that would start a download we don't want.
    var dataSaverEnabled by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        dataSaverEnabled = container.settings.current().dataSaverMode
    }

    // Resolve snackbar copy now -- stringResource is composable and can't
    // be called inside the action callbacks below.
    val wallpaperSetFailedMsg = stringResource(R.string.wallpaper_set_failed)

    // Smooth background: animate from transparent -> black over 220ms on enter,
    // synchronized with the shared element's bounds animation. The alpha is
    // read by DetailContent to color the background.
    val backgroundAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        backgroundAlpha.animateTo(1f, animationSpec = SharedElementSpringFloat)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = KraftColors.AuroraBlue)
            }
            uiState.error != null && wallpaper == null -> ErrorState(
                message = uiState.error ?: "",
                onRetry = viewModel::load,
                modifier = Modifier.fillMaxSize(),
            )
            wallpaper != null -> {
                if (wallpaper.path.isBlank()) {
                    ErrorState(
                        message = stringResource(R.string.wallpaper_load_failed),
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
                                boundsTransform = { _, _ -> SharedElementSpring },
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
                                scope.launch {
                                    val saved = container.favoriteImageStore.save(wallpaper)
                                    if (!saved) {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.favorite_save_failed),
                                        )
                                    }
                                }
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
                        onBack = onBack,
                        onTagClick = onTagClick,
                        onUploaderClick = onUploaderClick,
                        navBarPadding = navBarPadding,
                        modifier = Modifier.fillMaxSize(),
                        sharedElementModifier = sharedElementModifier,
                        animatedVisibilityScope = animatedVisibilityScope,
                        sharedTransitionScope = sharedTransitionScope,
                    )
                }
            }
        }

        androidx.compose.material3.SnackbarHost(
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
                // Resolution finished but no file -- show the failure and close.
                LaunchedEffect(Unit) {
                    setWallpaperTarget = null
                    snackbarHostState.showSnackbar(wallpaperSetFailedMsg)
                }
            }
        }
    }
}
