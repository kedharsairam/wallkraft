package com.wallkraft.app.presentation.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wallkraft.app.AppContainer
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.presentation.components.ErrorState
import com.wallkraft.app.presentation.components.FilterSheet
import com.wallkraft.app.presentation.components.EmptyState
import com.wallkraft.app.presentation.components.RateLimitBanner
import com.wallkraft.app.presentation.components.ShimmerGrid
import com.wallkraft.app.presentation.components.WallpaperGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    container: AppContainer,
    onOpenWallpaper: (String) -> Unit,
) {
    val viewModel: BrowseViewModel = viewModel(
        factory = viewModelFactory {
            initializer { BrowseViewModel(container.wallpaperRepository, container.settings) }
        },
    )
    val uiState by viewModel.uiState.collectAsState()
    var searchText by remember { mutableStateOf(uiState.query) }
    var showFilters by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("WallKraft") })
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = KraftSpacing.Spacing16, vertical = KraftSpacing.Spacing8),
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Search wallpapers") },
                        singleLine = true,
                        shape = RoundedCornerShapeDp,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchText.isNotEmpty()) {
                                IconButton(onClick = { searchText = ""; viewModel.search("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboard?.hide()
                                viewModel.search(searchText)
                            },
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(KraftSpacing.Spacing8))
                    IconButton(
                        onClick = { showFilters = true },
                        modifier = Modifier.padding(start = KraftSpacing.Spacing4),
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = "Filters")
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (uiState.rateLimited) {
                RateLimitBanner(modifier = Modifier.padding(horizontal = KraftSpacing.Spacing16))
                Spacer(Modifier.height(KraftSpacing.Spacing8))
            }

            when {
                uiState.isInitialLoading -> ShimmerGrid()
                uiState.error != null && uiState.wallpapers.isEmpty() ->
                    ErrorState(
                        message = uiState.error ?: "",
                        onRetry = viewModel::retry,
                    )
                uiState.wallpapers.isEmpty() ->
                    EmptyState(
                        title = "No wallpapers found",
                        message = if (uiState.query.isBlank()) {
                            "Try adjusting your filters."
                        } else {
                            "Nothing matched \"${uiState.query}\". Try a different search."
                        },
                    )
                else -> WallpaperGrid(
                    wallpapers = uiState.wallpapers,
                    onOpen = onOpenWallpaper,
                    onLoadMore = viewModel::loadNextPage,
                    footer = {
                        if (uiState.isAppending) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                            ) {
                                CircularProgressIndicator(modifier = Modifier.height(24.dp).width(24.dp))
                            }
                        }
                    },
                )
            }
        }
    }

    if (showFilters) {
        FilterSheet(
            initial = uiState.filters,
            onApply = { filters ->
                showFilters = false
                viewModel.setFilters(filters)
            },
            onDismiss = { showFilters = false },
        )
    }
}

private val RoundedCornerShapeDp = androidx.compose.foundation.shape.RoundedCornerShape(KraftRadius.Standard)
