package com.wallkraft.app.presentation.favorites

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wallkraft.app.AppContainer
import com.wallkraft.app.presentation.components.EmptyState
import com.wallkraft.app.presentation.components.WallpaperGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    container: AppContainer,
    onOpenWallpaper: (String) -> Unit,
) {
    val viewModel: FavoritesViewModel = viewModel(
        factory = viewModelFactory {
            initializer { FavoritesViewModel(container.favoritesRepository) }
        },
    )
    val favorites by viewModel.favorites.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Favorites") }) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (favorites.isEmpty()) {
                EmptyState(
                    title = "No favorites yet",
                    message = "Tap the heart on any wallpaper to save it here.",
                )
            } else {
                WallpaperGrid(
                    wallpapers = favorites.map { it.wallpaper },
                    onOpen = onOpenWallpaper,
                    onLoadMore = {},
                )
            }
        }
    }
}
