package com.wallkraft.app.presentation.detail

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import coil3.imageLoader
import coil3.request.ImageRequest
import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.domain.model.AppSettings
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftColors
import com.wallkraft.app.core.utils.KraftHaptics
import com.wallkraft.app.data.cache.OfflineImageStore
import com.wallkraft.app.data.prefs.CropStore
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.repository.SettingsRepository
import com.wallkraft.app.presentation.common.ConnectivityViewModel
import com.wallkraft.app.presentation.components.ErrorState
import com.wallkraft.app.presentation.components.OfflineBanner
import com.wallkraft.app.presentation.components.WallpaperCropDialog
import com.wallkraft.app.util.WallpaperDownload
import com.wallkraft.app.util.WallpaperSetter
import com.wallkraft.app.util.WallpaperSharing
import kotlinx.coroutines.launch
import java.io.File

internal val SharedElementSpring = spring<androidx.compose.ui.geometry.Rect>(dampingRatio = 0.7f, stiffness = 400f)
internal val SharedElementSpringFloat = spring<Float>(dampingRatio = 0.7f, stiffness = 400f)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreen(
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
    val viewModel: DetailViewModel = hiltViewModel()
    DetailScreenImpl(
        settingsRepository = viewModel.settingsRepository,
        favoriteImageStore = viewModel.favoriteImageStore,
        rotationCropStore = viewModel.rotationCropStore,
        wallpaperId = wallpaperId,
        onBack = onBack,
        onTagClick = onTagClick,
        onUploaderClick = onUploaderClick,
        navBarPadding = navBarPadding,
        previewThumb = previewThumb,
        previewPath = previewPath,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun DetailScreenImpl(
    settingsRepository: SettingsRepository,
    favoriteImageStore: OfflineImageStore,
    rotationCropStore: CropStore,
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
    val viewModel: DetailViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val connectivityViewModel: ConnectivityViewModel = hiltViewModel()
    val isOnline by connectivityViewModel.isOnline.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val wallpaper = uiState.wallpaper
    var setWallpaperTarget by remember { mutableStateOf<Wallpaper?>(null) }

    val wallpaperSetFailedMsg = stringResource(R.string.wallpaper_set_failed)

    val backgroundAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        backgroundAlpha.animateTo(1f, animationSpec = SharedElementSpringFloat)
    }

    // Adjacent preload — gated on dataSaverMode==false (mirror BrowseScreen.kt:138 prefetchFullRes).
    // Single-item detail (pager count 1) skips prefetch; prefetches populate cache cheaply, no cancel needed.
    val appSettings by settingsRepository.settings.collectAsState(initial = AppSettings())
    var currentPage by remember { mutableIntStateOf(0) }
    val pagerWallpapers = remember(wallpaper) { listOfNotNull(wallpaper) }
    LaunchedEffect(currentPage, pagerWallpapers.size, appSettings.dataSaverMode) {
        if (appSettings.dataSaverMode) return@LaunchedEffect
        if (pagerWallpapers.size <= 1) return@LaunchedEffect
        val imageLoader = context.imageLoader
        listOf(currentPage - 1, currentPage + 1).forEach { idx ->
            if (idx in pagerWallpapers.indices) {
                val url = pagerWallpapers[idx].path ?: return@forEach
                imageLoader.enqueue(
                    ImageRequest.Builder(context).data(url).size(KraftConstants.MaxDecodeDim).build(),
                )
            }
        }
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
                        favoriteImageStore = favoriteImageStore,
                        wallpaper = wallpaper,
                        isFavorite = wallpaper.id in uiState.favoriteIds,
                        isUploaderDeleted = uiState.isDetailLoaded && wallpaper.uploaderName.isBlank(),
                        imageModel = favoriteImageStore.fileFor(wallpaper.id) ?: wallpaper.path,
                        backgroundAlpha = backgroundAlpha.value,
                        onToggleFavorite = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val wasFavorite = wallpaper.id in uiState.favoriteIds
                            viewModel.toggleFavorite(wallpaper)
                            if (wasFavorite) {
                                favoriteImageStore.delete(wallpaper.id)
                            } else {
                                scope.launch {
                                    val saved = favoriteImageStore.save(wallpaper)
                                    if (!saved) {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.favorite_save_failed),
                                        )
                                    }
                                }
                            }
                        },
                        onDownload = {
                            KraftHaptics.buttonPress(haptic)
                            val downloadId = WallpaperDownload.download(context, wallpaper)
                            scope.launch {
                                if (downloadId >= 0) {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.downloading, wallpaper.resolution),
                                    )
                                } else {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.download_failed),
                                    )
                                }
                            }
                        },
                        onSetWallpaper = { setWallpaperTarget = wallpaper },
                        onBack = onBack,
                        onTagClick = onTagClick,
                        onUploaderClick = onUploaderClick,
                        navBarPadding = navBarPadding,
                        palette = uiState.palette,
                        related = uiState.related,
                        relatedLoading = uiState.relatedLoading,
                        onRelatedClick = { wp -> viewModel.swapToRelated(wp) },
                        onColorSwatchClick = { argb ->
                            val hex = String.format("%06X", 0xFFFFFF and argb)
                            onTagClick(hex)
                        },
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

        if (!isOnline && wallpaper != null) {
            OfflineBanner(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 68.dp, start = 16.dp, end = 16.dp),
            )
        }
    }

    val setTarget = setWallpaperTarget
    if (setTarget != null) {
        var resolvedFile by remember(setTarget) { mutableStateOf<File?>(null) }
        var resolving by remember(setTarget) { mutableStateOf(true) }
        LaunchedEffect(setTarget) {
            resolving = true
            resolvedFile = WallpaperSharing.imageFile(
                context,
                setTarget,
                favoriteImageStore.fileFor(setTarget.id),
            )
            resolving = false
        }
        val file = resolvedFile
        when {
            file != null -> WallpaperCropDialog(
                imageFile = file,
                onDismiss = { setWallpaperTarget = null },
                onConfirm = { cropped, position ->
                    val ok = WallpaperSetter.setAsWallpaper(context, cropped, position)
                    if (ok) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.recordHistory(setTarget, "MANUAL")
                    }
                    ok
                },
                onCropRect = { rect ->
                    scope.launch {
                        rotationCropStore.save(wallpaperId, rect)
                    }
                },
            )
            resolving -> {
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
                LaunchedEffect(Unit) {
                    setWallpaperTarget = null
                    snackbarHostState.showSnackbar(wallpaperSetFailedMsg)
                }
            }
        }
    }
}
