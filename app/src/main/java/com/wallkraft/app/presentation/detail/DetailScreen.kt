package com.wallkraft.app.presentation.detail

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wallkraft.app.AppContainer
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftColors
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.presentation.components.ErrorState
import com.wallkraft.app.presentation.components.ZoomableImage
import com.wallkraft.app.util.WallpaperActions
import com.wallkraft.app.util.toUserMessage
import com.wallkraft.app.util.wallpaperCategoryLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    container: AppContainer,
    wallpaperId: String,
    onBack: () -> Unit,
) {
    val viewModel: DetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                DetailViewModel(
                    id = wallpaperId,
                    wallpaperRepository = container.wallpaperRepository,
                    favoritesRepository = container.favoritesRepository,
                    errorMessage = { e -> e.toUserMessage(container.resources) },
                )
            }
        },
    )
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val wallpaper = uiState.wallpaper
    var isFullscreen by rememberSaveable { mutableStateOf(false) }

    // The fullscreen viewer takes over the entire screen and hides the
    // system bars. It reuses the same actions as the detail page below.
    if (isFullscreen && wallpaper != null) {
        // Resolve snackbar copy now — stringResource is composable and can't
        // be called inside the action callbacks below.
        val downloadingMsg = stringResource(R.string.downloading, wallpaper.resolution)
        val wallpaperSetMsg = stringResource(R.string.wallpaper_set)
        val wallpaperSetFailedMsg = stringResource(R.string.wallpaper_set_failed)
        FullscreenViewer(
            wallpaper = wallpaper,
            isFavorite = uiState.isFavorite,
            onToggleFavorite = viewModel::toggleFavorite,
            onDownload = {
                WallpaperActions.download(context, wallpaper)
                scope.launch { snackbarHostState.showSnackbar(downloadingMsg) }
            },
            onSetWallpaper = {
                scope.launch {
                    val ok = WallpaperActions.setAsWallpaper(context, wallpaper, container.okHttpClient)
                    snackbarHostState.showSnackbar(if (ok) wallpaperSetMsg else wallpaperSetFailedMsg)
                }
            },
            onShare = { WallpaperActions.share(context, wallpaper) },
            onExit = { isFullscreen = false },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(wallpaper?.resolution ?: stringResource(R.string.wallpaper)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (wallpaper != null) {
                        IconButton(onClick = { isFullscreen = true }) {
                            Icon(Icons.Filled.Fullscreen, contentDescription = stringResource(R.string.view_fullscreen))
                        }
                        IconButton(onClick = { WallpaperActions.share(context, wallpaper) }) {
                            Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share))
                        }
                        IconButton(onClick = { WallpaperActions.openInBrowser(context, wallpaper) }) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = stringResource(R.string.open_on_wallhaven))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            uiState.error != null && wallpaper == null -> ErrorState(
                message = uiState.error ?: "",
                onRetry = viewModel::load,
                modifier = Modifier.padding(innerPadding),
            )
            wallpaper != null -> {
                // Resolve snackbar copy here (not inside the action callbacks).
                val downloadingMsg = stringResource(R.string.downloading, wallpaper.resolution)
                val wallpaperSetMsg = stringResource(R.string.wallpaper_set)
                val wallpaperSetFailedMsg = stringResource(R.string.wallpaper_set_failed)
                DetailContent(
                    wallpaper = wallpaper,
                    isFavorite = uiState.isFavorite,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onDownload = {
                        WallpaperActions.download(context, wallpaper)
                        scope.launch { snackbarHostState.showSnackbar(downloadingMsg) }
                    },
                    onSetWallpaper = {
                        scope.launch {
                            val ok = WallpaperActions.setAsWallpaper(context, wallpaper, container.okHttpClient)
                            snackbarHostState.showSnackbar(if (ok) wallpaperSetMsg else wallpaperSetFailedMsg)
                        }
                    },
                    onOpenFullscreen = { isFullscreen = true },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    wallpaper: com.wallkraft.app.domain.model.Wallpaper,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    onSetWallpaper: () -> Unit,
    onOpenFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Same clamp as the grid tiles: guards against bad metadata (zero/negative
    // dimensions), which would otherwise produce aspectRatio(Infinity) and a
    // crash or a zero-height image.
    val w = wallpaper.dimensionX.toFloat().takeIf { it > 0f } ?: 1f
    val h = wallpaper.dimensionY.toFloat().takeIf { it > 0f } ?: 1f
    val ratio = (w / h).coerceIn(0.15f, 6f)

    LazyColumn(
        state = rememberLazyListState(),
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(KraftSpacing.Spacing16)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(KraftRadius.Large)),
            ) {
                ZoomableImage(
                    model = wallpaper.path,
                    contentDescription = wallpaper.resolution,
                    imageRatio = ratio,
                    onTap = onOpenFullscreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio),
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing12),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KraftSpacing.Spacing16, vertical = KraftSpacing.Spacing8),
            ) {
                FilledIconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) {
                            stringResource(R.string.remove_from_favorites)
                        } else {
                            stringResource(R.string.add_to_favorites)
                        },
                        tint = if (isFavorite) KraftColors.AccentRed else MaterialTheme.colorScheme.onPrimary,
                    )
                }
                FilledIconButton(onClick = onDownload) {
                    Icon(Icons.Filled.Download, contentDescription = stringResource(R.string.download))
                }
                FilledIconButton(onClick = onSetWallpaper) {
                    Icon(Icons.Filled.Wallpaper, contentDescription = stringResource(R.string.set_as_wallpaper))
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = KraftSpacing.Spacing16)) {
                Text(wallpaper.resolution, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(KraftSpacing.Spacing8))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                ) {
                    InfoChip(wallpaper.ratio)
                    InfoChip(wallpaper.fileSizeFormatted())
                    InfoChip(stringResource(R.string.favorites_format, wallpaper.favorites))
                    InfoChip(wallpaperCategoryLabel(wallpaper.category))
                }
                Spacer(Modifier.height(KraftSpacing.Spacing16))
                if (wallpaper.tags.isNotEmpty()) {
                    Text(
                        text = wallpaper.tags.joinToString("  ·  ") { "#${it.name}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(KraftSpacing.Spacing24))
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(KraftRadius.Small))
            .padding(horizontal = KraftSpacing.Spacing8, vertical = KraftSpacing.Spacing4),
    )
}

/**
 * Immersive fullscreen viewer: the image fills the whole screen, the system
 * bars are hidden (transient swipe to bring them back), and the chrome fades
 * away after a short idle period. Tap the image to toggle the chrome; the
 * back gesture/button exits back to the detail page.
 */
@Composable
private fun FullscreenViewer(
    wallpaper: com.wallkraft.app.domain.model.Wallpaper,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    onSetWallpaper: () -> Unit,
    onShare: () -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val window = (context as? Activity)?.window
    var controlsVisible by remember { mutableStateOf(true) }

    if (window != null) {
        val controller = remember(window) {
            WindowInsetsControllerCompat(window, window.decorView)
        }
        DisposableEffect(controller) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
        }
        // Keep the screen awake while viewing.
        DisposableEffect(window) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        }
    }

    // Auto-fade the chrome after a short idle period.
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(2500)
            controlsVisible = false
        }
    }

    BackHandler(onBack = onExit)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        ZoomableImage(
            model = wallpaper.path,
            contentDescription = wallpaper.resolution,
            imageRatio = wallpaper.dimensionX.toFloat().takeIf { it > 0f }?.div(
                wallpaper.dimensionY.toFloat().takeIf { it > 0f } ?: 1f,
            ) ?: 0f,
            modifier = Modifier.fillMaxSize(),
            onTap = { controlsVisible = !controlsVisible },
        )

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(Modifier.fillMaxSize()) {
                // Top chrome: back + resolution over a soft gradient.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
                            ),
                        )
                        .statusBarsPadding()
                        .padding(horizontal = KraftSpacing.Spacing8, vertical = KraftSpacing.Spacing8),
                ) {
                    IconButton(onClick = onExit) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White,
                        )
                    }
                    Text(
                        text = wallpaper.resolution,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                }

                // Bottom chrome: the usual actions over a soft gradient.
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                            ),
                        )
                        .navigationBarsPadding()
                        .padding(vertical = KraftSpacing.Spacing16),
                ) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) {
                                stringResource(R.string.remove_from_favorites)
                            } else {
                                stringResource(R.string.add_to_favorites)
                            },
                            tint = if (isFavorite) KraftColors.AccentRed else Color.White,
                        )
                    }
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Filled.Download, contentDescription = stringResource(R.string.download), tint = Color.White)
                    }
                    IconButton(onClick = onSetWallpaper) {
                        Icon(Icons.Filled.Wallpaper, contentDescription = stringResource(R.string.set_as_wallpaper), tint = Color.White)
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share), tint = Color.White)
                    }
                }
            }
        }
    }
}
