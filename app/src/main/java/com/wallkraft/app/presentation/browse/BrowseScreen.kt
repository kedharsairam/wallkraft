package com.wallkraft.app.presentation.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
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
import com.wallkraft.app.domain.model.Orientation
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.util.displayName
import com.wallkraft.app.util.WallpaperActions
import com.wallkraft.app.util.toUserMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    container: AppContainer,
    onOpenWallpaper: (Wallpaper) -> Unit,
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

    // Dropdown expansion state for the three filter buttons.
    var categoriesExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }
    var orientationExpanded by remember { mutableStateOf(false) }

    val filters = uiState.filters

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                // Search row: field (with magnifier + clear) + physical search button.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = KraftSpacing.Spacing16, vertical = KraftSpacing.Spacing8),
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
                            .height(44.dp)
                            .clip(RoundedCornerShapeDp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        decorationBox = { innerTextField ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = KraftSpacing.Spacing12),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(KraftSpacing.Spacing8))
                                Box(
                                    contentAlignment = Alignment.CenterStart,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    if (searchText.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.search_hint),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    innerTextField()
                                }
                                if (searchText.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchText = "" },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = stringResource(R.string.search_clear),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        },
                    )
                    Spacer(Modifier.width(KraftSpacing.Spacing8))
                    Button(
                        onClick = {
                            keyboard?.hide()
                            viewModel.search(searchText)
                        },
                        modifier = Modifier.height(44.dp),
                    ) {
                        Text(stringResource(R.string.search_action))
                    }
                }

                // Filter dropdown row: Categories, Sort, Orientation.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = KraftSpacing.Spacing16, vertical = KraftSpacing.Spacing4),
                ) {
                    FilterDropdownButton(
                        label = categoriesLabel(filters.categories),
                        expanded = categoriesExpanded,
                        onExpandedChange = { categoriesExpanded = it },
                        modifier = Modifier.weight(1f),
                    ) {
                        Category.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName()) },
                                leadingIcon = {
                                    if (cat in filters.categories) {
                                        Icon(Icons.Filled.Check, contentDescription = null)
                                    }
                                },
                                onClick = {
                                    val current = filters.categories
                                    val updated = if (cat in current) {
                                        // Never allow deselecting the last category —
                                        // an empty mask ("000") returns zero results.
                                        if (current.size > 1) current - cat else current
                                    } else {
                                        current + cat
                                    }
                                    viewModel.setFilters(filters.copy(categories = updated))
                                },
                            )
                        }
                    }
                    FilterDropdownButton(
                        label = filters.sorting.displayName(),
                        expanded = sortExpanded,
                        onExpandedChange = { sortExpanded = it },
                        modifier = Modifier.weight(1f),
                    ) {
                        Sorting.entries.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.displayName()) },
                                leadingIcon = {
                                    if (filters.sorting == s) {
                                        Icon(Icons.Filled.Check, contentDescription = null)
                                    }
                                },
                                onClick = {
                                    viewModel.setFilters(filters.copy(sorting = s))
                                    sortExpanded = false
                                },
                            )
                        }
                    }
                    FilterDropdownButton(
                        label = filters.orientation.displayName(),
                        expanded = orientationExpanded,
                        onExpandedChange = { orientationExpanded = it },
                        modifier = Modifier.weight(1f),
                    ) {
                        Orientation.entries.forEach { o ->
                            DropdownMenuItem(
                                text = { Text(o.displayName()) },
                                leadingIcon = {
                                    if (filters.orientation == o) {
                                        Icon(Icons.Filled.Check, contentDescription = null)
                                    }
                                },
                                onClick = {
                                    viewModel.setFilters(filters.copy(orientation = o))
                                    orientationExpanded = false
                                },
                            )
                        }
                    }
                }

                // Hairline separator so the header reads as a distinct surface
                // above the scrolling grid.
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
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

/**
 * A compact dropdown button for the filter row: a label + chevron that opens a
 * [DropdownMenu] anchored beneath it. The label always shows the current value
 * so the user knows what filter is applied at a glance.
 */
@Composable
private fun FilterDropdownButton(
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KraftRadius.Standard))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { onExpandedChange(!expanded) }
                .padding(horizontal = KraftSpacing.Spacing12, vertical = KraftSpacing.Spacing8),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            content()
        }
    }
}

/** Compact label for the categories dropdown: "All", a single name, or "X +N". */
@Composable
private fun categoriesLabel(categories: Set<Category>): String = when {
    categories.size == Category.entries.size -> stringResource(R.string.filter_all)
    categories.size == 1 -> categories.first().displayName()
    else -> "${categories.first().displayName()} +${categories.size - 1}"
}

private val RoundedCornerShapeDp = androidx.compose.foundation.shape.RoundedCornerShape(KraftRadius.Standard)