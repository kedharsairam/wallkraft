package com.wallkraft.app.presentation.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.wallkraft.app.AppContainer
import com.wallkraft.app.data.prefs.SearchHistoryStore
import com.wallkraft.app.di.AppDependenciesViewModel
import com.wallkraft.app.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.presentation.components.EmptyState
import com.wallkraft.app.presentation.components.ErrorState
import com.wallkraft.app.presentation.components.GridAppendFooter
import com.wallkraft.app.presentation.components.RateLimitBanner
import com.wallkraft.app.presentation.components.ShimmerGrid
import com.wallkraft.app.presentation.components.WallpaperGrid
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.util.DownloadedFiles
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun BrowseScreen(
    onOpenWallpaper: (Wallpaper) -> Unit,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState? = null,
    navBarPadding: androidx.compose.ui.unit.Dp = 0.dp,
    topInset: androidx.compose.ui.unit.Dp = 0.dp,
    initialQuery: String = "",
    title: String = "",
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
    searchState: BrowseSearchState = BrowseSearchState(),
) {
    val deps: AppDependenciesViewModel = hiltViewModel()
    BrowseScreenImpl(
        settingsRepository = deps.settingsRepository,
        searchHistoryStore = deps.searchHistoryStore,
        onOpenWallpaper = onOpenWallpaper,
        gridState = gridState,
        navBarPadding = navBarPadding,
        topInset = topInset,
        initialQuery = initialQuery,
        title = title,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        searchState = searchState,
    )
}

@Deprecated("Use Hilt version — container will be removed", ReplaceWith("BrowseScreen(onOpenWallpaper, gridState, navBarPadding, topInset, initialQuery, title, sharedTransitionScope, animatedVisibilityScope, searchState)"))
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun BrowseScreen(
    container: AppContainer,
    onOpenWallpaper: (Wallpaper) -> Unit,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState? = null,
    navBarPadding: androidx.compose.ui.unit.Dp = 0.dp,
    topInset: androidx.compose.ui.unit.Dp = 0.dp,
    initialQuery: String = "",
    title: String = "",
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
    searchState: BrowseSearchState = BrowseSearchState(),
) {
    BrowseScreenImpl(
        settingsRepository = container.settings,
        searchHistoryStore = container.searchHistory,
        onOpenWallpaper = onOpenWallpaper,
        gridState = gridState,
        navBarPadding = navBarPadding,
        topInset = topInset,
        initialQuery = initialQuery,
        title = title,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        searchState = searchState,
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
private fun BrowseScreenImpl(
    settingsRepository: SettingsRepository,
    searchHistoryStore: SearchHistoryStore,
    onOpenWallpaper: (Wallpaper) -> Unit,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState? = null,
    navBarPadding: androidx.compose.ui.unit.Dp = 0.dp,
    topInset: androidx.compose.ui.unit.Dp = 0.dp,
    initialQuery: String = "",
    title: String = "",
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
    searchState: BrowseSearchState = BrowseSearchState(),
) {
    val viewModel: BrowseViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val effectiveGridState = gridState ?: rememberLazyStaggeredGridState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(initialQuery, title) {
        searchState.query = title.ifBlank { uiState.filters.query }
        searchState.titleActive = title.isNotBlank()
    }
    searchState.filters = uiState.filters
    searchState.totalResults = uiState.totalResults
    val settings by settingsRepository.settings.collectAsState(initial = com.wallkraft.app.domain.model.AppSettings())
    searchState.hasApiKey = settings.apiKeyValid
    val history by searchHistoryStore.history.collectAsState(initial = emptyList())
    searchState.history = history
    searchState.onClearHistory = {
        scope.launch { searchHistoryStore.clear() }
    }
    searchState.onSearch = { text ->
        scope.launch { searchHistoryStore.add(text) }
        viewModel.search(if (searchState.titleActive) uiState.filters.query else text)
    }
    searchState.onFiltersChange = viewModel::setFilters
    var downloadedIds by remember { mutableStateOf(emptySet<String>()) }
    var prefetchFullRes by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        prefetchFullRes = !settingsRepository.current().dataSaverMode
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val ids = DownloadedFiles.downloadedIds(context)
                    withContext(Dispatchers.Main) {
                        downloadedIds = ids
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var lastScrolledFilters by remember { mutableStateOf(uiState.filters) }
    LaunchedEffect(uiState.filters) {
        val filters = uiState.filters
        if (filters != lastScrolledFilters) {
            lastScrolledFilters = filters
            effectiveGridState.animateScrollToItem(0)
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(effectiveGridState) {
        snapshotFlow { effectiveGridState.isScrollInProgress }
            .collect { scrolling ->
                if (scrolling) {
                    keyboard?.hide()
                    focusManager.clearFocus()
                }
            }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        // Pull indicator must be *below* the outer Glass top bar (which lives in
        // WallKraftNavHost outer Scaffold), not beneath it. Wrap the *whole*
        // inner content so the spinner is at the very top of the list area,
        // just under the top bar, and scrolls with the list.
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = {
                // Scroll to top so refresh is visible, then fetch.
                scope.launch { effectiveGridState.animateScrollToItem(0) }
                viewModel.refresh()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { focusManager.clearFocus() },
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (uiState.rateLimited) {
                        RateLimitBanner(
                            modifier = Modifier
                                .padding(horizontal = KraftSpacing.Spacing16)
                                .padding(top = topInset),
                        )
                        Spacer(Modifier.height(KraftSpacing.Spacing8))
                    }
                    val stateKey = when {
                        uiState.isInitialLoading -> "loading"
                        uiState.rateLimited && uiState.wallpapers.isEmpty() -> "rateLimited"
                        uiState.error != null && uiState.wallpapers.isEmpty() -> "error"
                        uiState.wallpapers.isEmpty() -> "empty"
                        else -> "grid"
                    }
                    Crossfade(
                        targetState = stateKey,
                        animationSpec = tween(durationMillis = 220),
                        label = "browseState",
                    ) { state ->
                        when (state) {
                            "loading" -> ShimmerGrid(
                                modifier = Modifier.padding(top = topInset),
                            )
                            "rateLimited" -> EmptyState(
                                title = stringResource(R.string.rate_limit_banner),
                                message = stringResource(R.string.rate_limit_hint),
                                icon = Icons.Outlined.Warning,
                                actionLabel = stringResource(R.string.error_retry),
                                onAction = viewModel::retry,
                                modifier = Modifier.padding(top = topInset),
                            )
                            "error" -> ErrorState(
                                message = uiState.error ?: "",
                                onRetry = viewModel::retry,
                                modifier = Modifier.padding(top = topInset),
                            )
                            "empty" -> EmptyState(
                                title = stringResource(R.string.no_results_title),
                                message = if (uiState.filters.query.isBlank()) {
                                    stringResource(R.string.no_results_hint_filters)
                                } else {
                                    stringResource(R.string.no_results_hint_query, uiState.filters.query)
                                },
                                modifier = Modifier.padding(top = topInset),
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
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    start = KraftSpacing.Spacing8,
                                    end = KraftSpacing.Spacing8,
                                    top = topInset + KraftSpacing.Spacing8,
                                    bottom = KraftSpacing.GlassBarReserve + navBarPadding,
                                ),
                                modifier = Modifier.fillMaxSize(),
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                            )
                        }
                    }
                }
            }
        }
    }
}
