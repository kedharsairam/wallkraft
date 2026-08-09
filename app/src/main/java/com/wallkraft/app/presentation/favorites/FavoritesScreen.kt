package com.wallkraft.app.presentation.favorites

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wallkraft.app.AppContainer
import com.wallkraft.app.R
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.presentation.components.EmptyState
import com.wallkraft.app.presentation.components.WallpaperGrid
import com.wallkraft.app.util.DownloadedFile
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
    // The set of downloaded IDs, so the grid can badge cards that are already
    // on disk. Refreshed on resume — a download from the detail screen must
    // show up without restarting the app.
    var downloadedFiles by remember { mutableStateOf(emptyMap<String, DownloadedFile>()) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // Data saver: skip the full-res prefetch on tap (favorites are already
    // local files, so the detail screen loads them instantly anyway).
    var prefetchFullRes by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        prefetchFullRes = !container.settings.current().dataSaverMode
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                downloadedFiles = WallpaperActions.downloadedFiles(context)
                    .associateBy { it.wallpaperId }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.favorites_title)) }) },
    ) { innerPadding ->
        if (favorites.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.no_favorites_title),
                message = stringResource(R.string.no_favorites_message),
                icon = Icons.Outlined.FavoriteBorder,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
        } else {
            WallpaperGrid(
                wallpapers = favorites.map { it.wallpaper },
                onOpen = onOpenWallpaper,
                onLoadMore = {},
                state = gridState,
                downloadedIds = downloadedFiles.keys,
                prefetchFullRes = prefetchFullRes,
                modifier = Modifier.padding(innerPadding).padding(bottom = navBarPadding),
            )
        }
    }
}