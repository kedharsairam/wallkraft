package com.wallkraft.app.presentation.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wallkraft.app.AppContainer
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.presentation.components.EmptyState
import com.wallkraft.app.presentation.components.ErrorState
import com.wallkraft.app.presentation.components.GridAppendFooter
import com.wallkraft.app.presentation.components.RateLimitBanner
import com.wallkraft.app.presentation.components.SearchFilterBar
import com.wallkraft.app.presentation.components.ShimmerGrid
import com.wallkraft.app.presentation.components.WallpaperGrid
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.util.WallpaperActions
import com.wallkraft.app.util.toUserMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    container: AppContainer,
    onOpenWallpaper: (Wallpaper) -> Unit,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState? = null,
    navBarPadding: androidx.compose.ui.unit.Dp = 0.dp,
    initialQuery: String = "",
) {
    val viewModel: BrowseViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                BrowseViewModel(
                    repository = container.wallpaperRepository,
                    settingsRepository = container.settings,
                    errorMessage = { e -> e.toUserMessage(container.resources) },
                    initialQuery = initialQuery,
                )
            }
        },
    )
    val uiState by viewModel.uiState.collectAsState()
    // The Browse tab hoists its grid state (so it survives tab switches); a
    // tag-as-browse entry passes null and gets its own state, so scrolling a
    // tag list never moves the original Browse position.
    val effectiveGridState = gridState ?: rememberLazyStaggeredGridState()
    var searchText by remember { mutableStateOf(uiState.query) }
    val keyboard = LocalSoftwareKeyboardController.current
    var downloadedIds by remember { mutableStateOf(emptySet<String>()) }
    // Data saver: skip the full-res prefetch on tap so opening a wallpaper
    // doesn't download it until the user actually zooms.
    var prefetchFullRes by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        prefetchFullRes = !container.settings.current().dataSaverMode
    }

    // Refresh downloaded IDs when screen becomes visible.
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        downloadedIds = WallpaperActions.downloadedIds(context)
    }

    // A new search or filter change replaces the whole list, so jump back to
    // the top instead of leaving the user staring at a stale scroll position.
    // Track the last-seen values so re-entering this tab (which recreates the
    // LaunchedEffect) doesn't reset the scroll — only an actual change should.
    var lastScrolledQuery by remember { mutableStateOf(uiState.query) }
    var lastScrolledFilters by remember { mutableStateOf(uiState.filters) }
    LaunchedEffect(uiState.query, uiState.filters) {
        val query = uiState.query
        val filters = uiState.filters
        if (query != lastScrolledQuery || filters != lastScrolledFilters) {
            lastScrolledQuery = query
            lastScrolledFilters = filters
            effectiveGridState.scrollToItem(0)
        }
    }

    // Dismiss the keyboard when the user starts scrolling the grid.
    LaunchedEffect(effectiveGridState) {
        snapshotFlow { effectiveGridState.isScrollInProgress }
            .collect { scrolling -> if (scrolling) keyboard?.hide() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SearchFilterBar(
                query = searchText,
                onQueryChange = { searchText = it },
                onSearch = { viewModel.search(it) },
                filters = uiState.filters,
                onFiltersChange = viewModel::setFilters,
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (uiState.rateLimited) {
                    RateLimitBanner(modifier = Modifier.padding(horizontal = KraftSpacing.Spacing16))
                    Spacer(Modifier.height(KraftSpacing.Spacing8))
                }

                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds(),
                ) {
                    when {
                        uiState.isInitialLoading -> ShimmerGrid()
                        uiState.error != null && uiState.wallpapers.isEmpty() ->
                            ErrorState(
                                message = uiState.error ?: "",
                                onRetry = viewModel::retry,
                            )
                        uiState.wallpapers.isEmpty() ->
                            EmptyState(
                                title = stringResource(R.string.no_results_title),
                                message = if (uiState.query.isBlank()) {
                                    stringResource(R.string.no_results_hint_filters)
                                } else {
                                    stringResource(R.string.no_results_hint_query, uiState.query)
                                },
                            )
                        else -> WallpaperGrid(
                            wallpapers = uiState.wallpapers,
                            onOpen = onOpenWallpaper,
                            onLoadMore = viewModel::loadNextPage,
                            state = effectiveGridState,
                            downloadedIds = downloadedIds,
                            prefetchFullRes = prefetchFullRes,
                            footer = {
                                if (uiState.isAppending) GridAppendFooter()
                            },
                            modifier = Modifier.padding(bottom = navBarPadding),
                        )
                    }
                }
            }
        }
    }
}