package com.wallkraft.app.presentation.favorites

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wallkraft.app.AppContainer
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.presentation.components.EmptyState
import com.wallkraft.app.presentation.components.WallpaperGrid
import com.wallkraft.app.util.WallpaperActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    container: AppContainer,
    onOpenWallpaper: (Wallpaper) -> Unit,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
    navBarPadding: Dp = 0.dp,
) {
    val viewModel: FavoritesViewModel = viewModel(
        factory = viewModelFactory {
            initializer { FavoritesViewModel(container.favoritesRepository) }
        },
    )
    val favorites by viewModel.favorites.collectAsState()
    var downloadedIds by remember { mutableStateOf(emptySet<String>()) }
    // "All" vs "Downloaded" — a hidden offline library inside Favorites.
    var showDownloadedOnly by remember { mutableStateOf(false) }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        downloadedIds = WallpaperActions.downloadedIds(context)
    }

    val visible = if (showDownloadedOnly) {
        favorites.filter { it.wallpaper.id in downloadedIds }
    } else {
        favorites
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.favorites_title)) }) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (favorites.isNotEmpty()) {
                // Offline filter: All / Downloaded.
                Row(modifier = Modifier.padding(horizontal = KraftSpacing.Spacing16)) {
                    FilterChip(
                        selected = !showDownloadedOnly,
                        onClick = { showDownloadedOnly = false },
                        label = { Text(stringResource(R.string.favorites_all)) },
                    )
                    Spacer(Modifier.width(KraftSpacing.Spacing8))
                    FilterChip(
                        selected = showDownloadedOnly,
                        onClick = { showDownloadedOnly = true },
                        label = { Text(stringResource(R.string.favorites_downloaded)) },
                    )
                }
                Spacer(Modifier.height(KraftSpacing.Spacing8))
            }

            if (visible.isEmpty()) {
                EmptyState(
                    title = stringResource(
                        if (showDownloadedOnly) R.string.no_downloaded_title else R.string.no_favorites_title,
                    ),
                    message = stringResource(
                        if (showDownloadedOnly) R.string.no_downloaded_message else R.string.no_favorites_message,
                    ),
                    icon = Icons.Outlined.FavoriteBorder,
                )
            } else {
                WallpaperGrid(
                    wallpapers = visible.map { it.wallpaper },
                    onOpen = onOpenWallpaper,
                    onLoadMore = {},
                    state = gridState,
                    downloadedIds = downloadedIds,
                    modifier = Modifier.padding(bottom = navBarPadding),
                )
            }
        }
    }
}