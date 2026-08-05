package com.wallkraft.app.presentation.browse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wallkraft.app.AppContainer
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.presentation.components.ErrorState
import com.wallkraft.app.presentation.components.EmptyState
import com.wallkraft.app.presentation.components.GridAppendFooter
import com.wallkraft.app.presentation.components.RateLimitBanner
import com.wallkraft.app.presentation.components.ShimmerGrid
import com.wallkraft.app.presentation.components.WallpaperGrid
import com.wallkraft.app.domain.model.Category
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.domain.model.Order
import com.wallkraft.app.domain.model.TopRange
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.util.displayName
import com.wallkraft.app.util.WallpaperActions
import com.wallkraft.app.util.toUserMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    container: AppContainer,
    onOpenWallpaper: (String) -> Unit,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
    navBarPadding: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val viewModel: BrowseViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                BrowseViewModel(
                    repository = container.wallpaperRepository,
                    settingsRepository = container.settings,
                    errorMessage = { e -> e.toUserMessage(container.resources) },
                )
            }
        },
    )
    val uiState by viewModel.uiState.collectAsState()
    var searchText by remember { mutableStateOf(uiState.query) }
    var showFilters by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    var downloadedIds by remember { mutableStateOf(emptySet<String>()) }

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
            gridState.scrollToItem(0)
        }
    }

    // Dismiss the keyboard when the user starts scrolling the grid.
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress }
            .collect { scrolling -> if (scrolling) keyboard?.hide() }
    }

    // The filter menu overlay lives outside the Scaffold so it can cover the
    // full screen (scrim + slide-in panel). The Scaffold handles the topBar and
    // content; the overlay is layered on top via a Box wrapper.
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                // One compact bar: the search field and filter button in a single
                // row. Search is the primary action, so it owns the full width.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = KraftSpacing.Spacing12, vertical = KraftSpacing.Spacing8),
                ) {
                    BasicTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Search,
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboard?.hide()
                                viewModel.search(searchText)
                            },
                        ),
                        textStyle = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShapeDp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        decorationBox = { innerTextField ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = KraftSpacing.Spacing12),
                            ) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                                Box(modifier = Modifier.weight(1f).padding(start = KraftSpacing.Spacing8)) {
                                    if (searchText.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.search_hint),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    innerTextField()
                                }
                                if (searchText.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchText = ""; viewModel.search("") },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = stringResource(R.string.search_clear),
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        },
                    )
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.filter_title))
                    }
                }
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
                                state = gridState,
                                downloadedIds = downloadedIds,
                                modifier = Modifier.padding(bottom = navBarPadding),
                                footer = {
                                    if (uiState.isAppending) {
                                        GridAppendFooter()
                                    }
                                },
                            )
                        }
                    }
                }

                // Filter overlay: scrim + slide-in panel, layered on top of content.
                FilterDropdownMenu(
                    expanded = showFilters,
                    onDismiss = { showFilters = false },
                    initial = uiState.filters,
                    onApply = { filters ->
                        showFilters = false
                        viewModel.setFilters(filters)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    initial: WallhavenFilters,
    onApply: (WallhavenFilters) -> Unit,
) {
    var categories by remember { mutableStateOf(initial.categories) }
    var sorting by remember { mutableStateOf(initial.sorting) }
    var order by remember { mutableStateOf(initial.order) }
    var topRange by remember { mutableStateOf(initial.topRange) }

    val chipBorder = FilterChipDefaults.filterChipBorder(
        enabled = true,
        selected = false,
        borderColor = MaterialTheme.colorScheme.outlineVariant,
        selectedBorderColor = Color.Transparent,
        borderWidth = 1.dp,
        selectedBorderWidth = 0.dp,
    )

    // Scrim + menu panel combined in a single overlay.
    // Scrim fades in/out. Menu slides down from the top edge (clipped).
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(200)),
    ) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
    }

    AnimatedVisibility(
        visible = expanded,
        enter = slideInVertically(
            initialOffsetY = { -it / 6 },
            animationSpec = tween(300, easing = FastOutSlowInEasing),
        ) + fadeIn(animationSpec = tween(250)),
        exit = slideOutVertically(
            targetOffsetY = { -it / 6 },
            animationSpec = tween(250, easing = FastOutSlowInEasing),
        ) + fadeOut(animationSpec = tween(200)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .shadow(8.dp, RoundedCornerShape(bottomStart = KraftRadius.Large, bottomEnd = KraftRadius.Large))
                .clip(RoundedCornerShape(bottomStart = KraftRadius.Large, bottomEnd = KraftRadius.Large))
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(modifier = Modifier.padding(horizontal = KraftSpacing.Spacing16, vertical = KraftSpacing.Spacing14)) {
                // Header
                Text(
                    text = stringResource(R.string.filter_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(KraftSpacing.Spacing14))

                // Categories
                Text(
                    stringResource(R.string.filter_categories),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(KraftSpacing.Spacing4))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing4),
                ) {
                    Category.entries.forEach { cat ->
                        FilterChip(
                            selected = cat in categories,
                            onClick = { if (cat !in categories || categories.size > 1) categories = categories.toggle(cat) },
                            label = { Text(cat.displayName(), style = MaterialTheme.typography.labelSmall) },
                            border = chipBorder,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }
                }

                // Sort By
                Spacer(Modifier.height(KraftSpacing.Spacing14))
                Text(
                    stringResource(R.string.filter_sort_by),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(KraftSpacing.Spacing4))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing4),
                ) {
                    Sorting.entries.forEach { s ->
                        FilterChip(
                            selected = sorting == s,
                            onClick = { sorting = s; if (s != Sorting.Toplist) topRange = null },
                            label = { Text(s.displayName(), style = MaterialTheme.typography.labelSmall) },
                            border = chipBorder,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }
                }

                // Top Range (only if Toplist selected)
                if (sorting == Sorting.Toplist) {
                    Spacer(Modifier.height(KraftSpacing.Spacing14))
                    Text(
                        stringResource(R.string.filter_time_range),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(KraftSpacing.Spacing4))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                        verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing4),
                    ) {
                        TopRange.entries.forEach { r ->
                            FilterChip(
                                selected = topRange == r,
                                onClick = { topRange = if (topRange == r) null else r },
                                label = { Text(r.displayName(), style = MaterialTheme.typography.labelSmall) },
                                border = chipBorder,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                ),
                            )
                        }
                    }
                }

                // Order
                Spacer(Modifier.height(KraftSpacing.Spacing14))
                Text(
                    stringResource(R.string.filter_order),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(KraftSpacing.Spacing4))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing4),
                ) {
                    Order.entries.forEach { o ->
                        FilterChip(
                            selected = order == o,
                            onClick = { order = o },
                            label = { Text(o.displayName(), style = MaterialTheme.typography.labelSmall) },
                            border = chipBorder,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }
                }

                // Apply button
                Spacer(Modifier.height(KraftSpacing.Spacing16))
                Button(
                    onClick = {
                        onApply(
                            WallhavenFilters(
                                categories = categories,
                                sorting = sorting,
                                order = order,
                                topRange = if (sorting == Sorting.Toplist) topRange else null,
                                ratio = initial.ratio,
                                query = initial.query,
                            ),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(KraftRadius.Standard),
                ) {
                    Text(
                        stringResource(R.string.filter_apply),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> =
    if (value in this) this - value else this + value

private val RoundedCornerShapeDp = androidx.compose.foundation.shape.RoundedCornerShape(KraftRadius.Standard)