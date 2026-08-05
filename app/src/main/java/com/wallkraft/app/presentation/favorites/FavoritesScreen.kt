package com.wallkraft.app.presentation.favorites

import androidx.compose.foundation.layout.Column
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
import com.wallkraft.app.presentation.components.EmptyState
import com.wallkraft.app.presentation.components.WallpaperGrid
import com.wallkraft.app.util.WallpaperActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    container: AppContainer,
    onOpenWallpaper: (String) -> Unit,
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
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        downloadedIds = WallpaperActions.downloadedIds(context)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.favorites_title)) }) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (favorites.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.no_favorites_title),
                    message = stringResource(R.string.no_favorites_message),
                    icon = Icons.Outlined.FavoriteBorder,
                )
            } else {
                WallpaperGrid(
                    wallpapers = favorites.map { it.wallpaper },
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
