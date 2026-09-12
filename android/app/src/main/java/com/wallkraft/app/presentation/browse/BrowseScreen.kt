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
    container: AppContainer,
    onOpenWallpaper: (Wallpaper) -> Unit,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState? = null,
    navBarPadding: androidx.compose.ui.unit.Dp = 0.dp,
    // Height of the outer top bar (SearchFilterBar). Reserved here so the
    // grid starts exactly below the bar. Constant — never shifts.
    topInset: androidx.compose.ui.unit.Dp = 0.dp,
    initialQuery: String = "",
    title: String = "",
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
    searchState: BrowseSearchState = BrowseSearchState(),
) {
    // Hilt pilot: BrowseViewModel is provided by Hilt. Nav argument "query" is read
    // via SavedStateHandle inside the ViewModel, so we no longer pass initialQuery
    // or container resources into a manual factory. Container is still used for
    // settings/history/download state (remaining screens migrate next).
    val viewModel: BrowseViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    // The Browse tab hoists its grid state (so it survives tab switches); a
    // tag-as-browse entry passes null and gets its own state, so scrolling a
    // tag list never moves the original Browse position.
    val effectiveGridState = gridState ?: rememberLazyStaggeredGridState()
    // Sync shared search state (lives outside SharedTransitionLayout).
    // Initial query from nav args seeds the field once per entry.
    val scope = rememberCoroutineScope()
    LaunchedEffect(initialQuery, title) {
        searchState.query = title.ifBlank { uiState.filters.query }
        searchState.titleActive = title.isNotBlank()
        // Tag/uploader entries are NOT recorded — history holds only what
        // Kedhar explicitly searched (typed + submitted via onSearch below).
    }
    searchState.filters = uiState.filters
    searchState.totalResults = uiState.totalResults
    val settings by container.settings.settings.collectAsState(initial = com.wallkraft.app.domain.model.AppSettings())
    searchState.hasApiKey = settings.apiKeyValid
    // Suggestion source for the outer bar: explicit search history only.
    // (Session tags from loaded wallpapers are intentionally NOT suggested —
    // the dropdown shows only what Kedhar typed and searched.)
    val history by container.searchHistory.history.collectAsState(initial = emptyList())
    searchState.history = history
    searchState.onClearHistory = {
        scope.launch { container.searchHistory.clear() }
    }
    searchState.onSearch = { text ->
        scope.launch { container.searchHistory.add(text) }
        viewModel.search(if (searchState.titleActive) uiState.filters.query else text)
    }
    searchState.onFiltersChange = viewModel::setFilters
    var downloadedIds by remember { mutableStateOf(emptySet<String>()) }
    // Data saver: skip the full-res prefetch on tap so opening a wallpaper
    // doesn't download it until the user actually zooms.
    var prefetchFullRes by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        prefetchFullRes = !container.settings.current().dataSaverMode
    }

    // Refresh downloaded IDs when screen becomes visible — a wallpaper downloaded
    // from the detail screen (or outside the app) should show its badge immediately
    // when the user returns to Browse, without needing to restart.
    // MediaStore query can be heavy, so we do it off the main thread.
    // Both ON_START and ON_RESUME are observed to catch every return path
    // (e.g. multi-window, split-screen, notification overlay).
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                // Fire-and-forget off main thread to avoid jank when resuming.
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

    // A new search or filter change replaces the whole list, so jump back to
    // the top instead of leaving the user staring at a stale scroll position.
    // Track the last-seen values so re-entering this tab (which recreates the
    // LaunchedEffect) doesn't reset the scroll — only an actual change should.
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

    // Dismiss the keyboard + clear cursor when the user starts scrolling the grid.
    LaunchedEffect(effectiveGridState) {
        snapshotFlow { effectiveGridState.isScrollInProgress }
            .collect { scrolling ->
                if (scrolling) {
                    keyboard?.hide()
                    focusManager.clearFocus()
                }
            }
    }

    // Full-bleed behind frosted top — no Scaffold topBar spacer.
    // The grid's contentPadding handles topInset so at rest the first tile
    // sits just below the frost, but on scroll tiles draw behind it and
    // get blurred like the bottom pill. outer Scaffold still reserves topInset
    // for measurement only.
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
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

                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier
                        .fillMaxSize(),
                ) {
                    // Crossfade between states for smooth transitions (content changes should feel cohesive).
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