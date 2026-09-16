/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Kedhar Sairam
 */
package com.wallkraft.app.presentation.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.wallkraft.app.data.prefs.SearchHistoryRepository
import com.wallkraft.app.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.core.utils.rememberReduceMotion
import com.wallkraft.app.presentation.components.EmptyState
import com.wallkraft.app.presentation.components.GridAppendFooter
import com.wallkraft.app.presentation.components.PaginationErrorFooter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallkraft.app.presentation.common.ConnectivityViewModel
import com.wallkraft.app.presentation.components.OfflineBanner
import com.wallkraft.app.presentation.components.RateLimitBanner
import com.wallkraft.app.presentation.components.ShimmerGrid
import com.wallkraft.app.presentation.components.WallpaperGrid
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.util.DownloadedFiles
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.SearchOff
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
    viewModel: BrowseViewModel = hiltViewModel(),
    connectivityViewModel: ConnectivityViewModel = hiltViewModel(),
) {
    BrowseScreenImpl(
        settingsRepository = viewModel.settingsRepository,
        searchHistoryStore = viewModel.searchHistoryStore,
        onOpenWallpaper = onOpenWallpaper,
        gridState = gridState,
        navBarPadding = navBarPadding,
        topInset = topInset,
        initialQuery = initialQuery,
        title = title,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        searchState = searchState,
        viewModel = viewModel,
        connectivityViewModel = connectivityViewModel,
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
private fun BrowseScreenImpl(
    settingsRepository: SettingsRepository,
    searchHistoryStore: SearchHistoryRepository,
    onOpenWallpaper: (Wallpaper) -> Unit,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState? = null,
    navBarPadding: androidx.compose.ui.unit.Dp = 0.dp,
    topInset: androidx.compose.ui.unit.Dp = 0.dp,
    initialQuery: String = "",
    title: String = "",
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
    searchState: BrowseSearchState = BrowseSearchState(),
    viewModel: BrowseViewModel = hiltViewModel(),
    connectivityViewModel: ConnectivityViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isOnline by connectivityViewModel.isOnline.collectAsStateWithLifecycle()
    val effectiveGridState = gridState ?: rememberLazyStaggeredGridState()
    val scope = rememberCoroutineScope()
    val reduceMotion = rememberReduceMotion()
    LaunchedEffect(initialQuery, title) {
        searchState.query = title.ifBlank { uiState.filters.query }
        searchState.titleActive = title.isNotBlank()
    }
    searchState.filters = uiState.filters
    searchState.totalResults = uiState.totalResults
    val apiKeyFlow = remember(settingsRepository) {
        settingsRepository.settings
            .map { it.apiKeyValid }
            .distinctUntilChanged()
    }
    val apiKeyValid by apiKeyFlow.collectAsState(initial = com.wallkraft.app.domain.model.AppSettings().apiKeyValid)
    searchState.hasApiKey = apiKeyValid
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
    var downloadedIdsLoaded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        prefetchFullRes = !settingsRepository.current().dataSaverMode
        // MediaStore query does binder + disk I/O — never on Main on slow devices.
        val ids = withContext(Dispatchers.IO) { DownloadedFiles.downloadedIds(context) }
        downloadedIds = ids
        downloadedIdsLoaded = true
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && downloadedIdsLoaded) {
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
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

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
                .padding(bottom = innerPadding.calculateBottomPadding())
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { focusManager.clearFocus() },
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(top = topInset)) {
                if (!isOnline) {
                    OfflineBanner(
                        modifier = Modifier.padding(horizontal = KraftSpacing.Spacing16),
                    )
                    Spacer(Modifier.height(KraftSpacing.Spacing8))
                }
                if (uiState.rateLimited) {
                    RateLimitBanner(
                        modifier = Modifier.padding(horizontal = KraftSpacing.Spacing16),
                    )
                    Spacer(Modifier.height(KraftSpacing.Spacing8))
                }
                if (uiState.appliedFilters != null) {
                    PurityFallbackBanner(
                        onShowOriginal = viewModel::showOriginalFilters,
                        modifier = Modifier.padding(horizontal = KraftSpacing.Spacing16),
                    )
                    Spacer(Modifier.height(KraftSpacing.Spacing8))
                }
                if (uiState.error != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = KraftSpacing.Spacing16)
                            .background(
                                MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.medium,
                            )
                            .padding(horizontal = KraftSpacing.Spacing16, vertical = KraftSpacing.Spacing12),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing12),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            text = uiState.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = viewModel::retry) {
                            Text(stringResource(R.string.error_retry))
                        }
                    }
                    Spacer(Modifier.height(KraftSpacing.Spacing8))
                }
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        viewModel.refresh()
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    val listCachedAt = uiState.cachedAt
                    val staleCaptionVisible =
                        !uiState.isInitialLoading &&
                            !uiState.isRefreshing &&
                            uiState.wallpapers.isNotEmpty() &&
                            listCachedAt != null &&
                            System.currentTimeMillis() - listCachedAt > KraftConstants.SearchCacheTtlMs
                    val stateKey = when {
                        uiState.isInitialLoading -> "loading"
                        uiState.rateLimited && uiState.wallpapers.isEmpty() -> "rateLimited"
                        uiState.wallpapers.isEmpty() -> "empty"
                        else -> "grid"
                    }
                    // Show pagination error snackbar when data exists but load-more failed
                    val paginationError = uiState.error != null && uiState.wallpapers.isNotEmpty()
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Stale-data caption: subtle, tappable "Updated X ago"
                        // under the browse header. Shown only when stale data is
                        // actually visible — never on fresh loads, loading, or
                        // empty states. Stale != broken, so no warning colors;
                        // red/amber stay reserved for the offline/error states.
                        if (listCachedAt != null && staleCaptionVisible) {
                            StaleUpdatedCaption(
                                cachedAt = listCachedAt,
                                onRetry = viewModel::refresh,
                            )
                        }
                        Crossfade(
                            targetState = stateKey,
                            animationSpec = tween(durationMillis = if (reduceMotion) 0 else 220),
                            label = "browseState",
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        ) { state ->
                        when (state) {
                            "loading" -> ShimmerGrid(
                                modifier = Modifier,
                            )
                            "rateLimited" -> EmptyState(
                                title = stringResource(R.string.rate_limit_banner),
                                message = stringResource(R.string.rate_limit_hint),
                                icon = Icons.Outlined.Warning,
                                actionLabel = stringResource(R.string.error_retry),
                                onAction = viewModel::retry,
                                modifier = Modifier,
                            )
                            "empty" -> EmptyState(
                                title = stringResource(R.string.no_results_found),
                                message = if (uiState.filters.query.isBlank()) {
                                    stringResource(R.string.no_results_search_hint)
                                } else {
                                    stringResource(R.string.no_results_search_hint)
                                },
                                icon = Icons.Outlined.SearchOff,
                                modifier = Modifier,
                            )
                            else -> WallpaperGrid(
                                wallpapers = uiState.wallpapers,
                                onOpen = onOpenWallpaper,
                                onLoadMore = viewModel::loadNextPage,
                                state = effectiveGridState,
                                downloadedIds = downloadedIds,
                                prefetchFullRes = prefetchFullRes,
                                footer = {
                                    when {
                                        uiState.isAppending -> GridAppendFooter()
                                        paginationError -> PaginationErrorFooter(
                                            message = uiState.error ?: "",
                                            onRetry = viewModel::loadNextPage,
                                        )
                                    }
                                },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    start = KraftSpacing.Spacing16,
                                    end = KraftSpacing.Spacing16,
                                    top = KraftSpacing.Spacing8,
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
}

/**
 * Subtle stale-data caption shown directly under the browse header when the
 * visible results are older than the search-cache TTL. Plain caption styling
 * on the default background — deliberately not a warning banner (stale data
 * is still usable; red/amber are reserved for offline/error states). Tapping
 * forces a refresh.
 */
@Composable
private fun StaleUpdatedCaption(
    cachedAt: Long,
    onRetry: () -> Unit,
) {
    val ago = remember(cachedAt) { com.wallkraft.app.presentation.detail.relativeTimeAgo(cachedAt) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRetry)
            .padding(horizontal = KraftSpacing.Spacing16, vertical = KraftSpacing.Spacing4),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.browse_updated_ago, ago),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/**
 * Banner shown when results were returned from a broader purity/category
 * filter set because the user's original filters yielded zero results.
 */
@Composable
private fun PurityFallbackBanner(
    onShowOriginal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KraftRadius.Standard))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }
            .padding(horizontal = KraftSpacing.Spacing16, vertical = KraftSpacing.Spacing12),
    ) {
        Text(
            text = stringResource(R.string.purity_fallback_banner),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onShowOriginal) {
            Text(stringResource(R.string.purity_fallback_show_original))
        }
    }
}
