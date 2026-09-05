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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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
import com.wallkraft.app.util.WallpaperActions
import com.wallkraft.app.util.toUserMessage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun BrowseScreen(
    container: AppContainer,
    onOpenWallpaper: (Wallpaper) -> Unit,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState? = null,
    navBarPadding: androidx.compose.ui.unit.Dp = 0.dp,
    initialQuery: String = "",
    title: String = "",
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
    searchState: BrowseSearchState = BrowseSearchState(),
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
    // Sync shared search state (lives outside SharedTransitionLayout).
    // Initial query from nav args seeds the field once per entry.
    LaunchedEffect(initialQuery, title) {
        searchState.query = title.ifBlank { uiState.filters.query }
        searchState.titleActive = title.isNotBlank()
    }
    searchState.filters = uiState.filters
    val settings by container.settings.settings.collectAsState(initial = com.wallkraft.app.domain.model.AppSettings())
    searchState.hasApiKey = settings.apiKeyValid
    searchState.onSearch = { text ->
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
                    val ids = WallpaperActions.downloadedIds(context)
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { focusManager.clearFocus() },
        ) {
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
                            "loading" -> ShimmerGrid()
                            "rateLimited" -> EmptyState(
                                title = stringResource(R.string.rate_limit_banner),
                                message = stringResource(R.string.rate_limit_hint),
                                icon = Icons.Outlined.Warning,
                                actionLabel = stringResource(R.string.error_retry),
                                onAction = viewModel::retry,
                            )
                            "error" -> ErrorState(
                                message = uiState.error ?: "",
                                onRetry = viewModel::retry,
                            )
                            "empty" -> EmptyState(
                                title = stringResource(R.string.no_results_title),
                                message = if (uiState.filters.query.isBlank()) {
                                    stringResource(R.string.no_results_hint_filters)
                                } else {
                                    stringResource(R.string.no_results_hint_query, uiState.filters.query)
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