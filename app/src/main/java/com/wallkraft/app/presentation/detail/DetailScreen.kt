package com.wallkraft.app.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil3.compose.AsyncImage
import com.wallkraft.app.AppContainer
import com.wallkraft.app.core.design.KraftColors
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.presentation.components.ErrorState
import com.wallkraft.app.util.WallpaperActions
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
                )
            }
        },
    )
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val wallpaper = uiState.wallpaper

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(wallpaper?.resolution ?: "Wallpaper") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (wallpaper != null) {
                        IconButton(onClick = { WallpaperActions.share(context, wallpaper) }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = { WallpaperActions.openInBrowser(context, wallpaper) }) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = "Open on wallhaven.cc")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(innerPadding))
            uiState.error != null && wallpaper == null -> ErrorState(
                message = uiState.error ?: "",
                onRetry = viewModel::load,
                modifier = Modifier.padding(innerPadding),
            )
            wallpaper != null -> DetailContent(
                wallpaper = wallpaper,
                isFavorite = uiState.isFavorite,
                onToggleFavorite = viewModel::toggleFavorite,
                onDownload = {
                    WallpaperActions.download(context, wallpaper)
                    scope.launch { snackbarHostState.showSnackbar("Downloading ${wallpaper.resolution}…") }
                },
                onSetWallpaper = {
                    WallpaperActions.setAsWallpaper(
                        context = context,
                        wallpaper = wallpaper,
                        client = container.okHttpClient,
                        onResult = { ok ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (ok) "Wallpaper set" else "Couldn't set wallpaper",
                                )
                            }
                        },
                    )
                },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun DetailContent(
    wallpaper: com.wallkraft.app.domain.model.Wallpaper,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    onSetWallpaper: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                AsyncImage(
                    model = wallpaper.thumbnailLarge ?: wallpaper.thumbnail,
                    contentDescription = wallpaper.resolution,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(wallpaper.dimensionX.toFloat() / wallpaper.dimensionY.toFloat())
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
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) KraftColors.AccentRed else MaterialTheme.colorScheme.onPrimary,
                    )
                }
                FilledIconButton(onClick = onDownload) {
                    Icon(Icons.Filled.Download, contentDescription = "Download")
                }
                FilledIconButton(onClick = onSetWallpaper) {
                    Icon(Icons.Filled.Wallpaper, contentDescription = "Set as wallpaper")
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = KraftSpacing.Spacing16)) {
                Text(wallpaper.resolution, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(KraftSpacing.Spacing8))
                Row {
                    InfoChip(wallpaper.ratio)
                    Spacer(Modifier.width(KraftSpacing.Spacing8))
                    InfoChip(wallpaper.fileSizeFormatted())
                    Spacer(Modifier.width(KraftSpacing.Spacing8))
                    InfoChip("${wallpaper.favorites} ♥")
                    Spacer(Modifier.width(KraftSpacing.Spacing8))
                    InfoChip(wallpaper.category)
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
