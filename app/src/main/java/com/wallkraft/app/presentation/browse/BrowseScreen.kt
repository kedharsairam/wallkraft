package com.wallkraft.app.presentation.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import com.wallkraft.app.presentation.components.FilterSheet
import com.wallkraft.app.presentation.components.EmptyState
import com.wallkraft.app.presentation.components.RateLimitBanner
import com.wallkraft.app.presentation.components.ShimmerGrid
import com.wallkraft.app.presentation.components.WallpaperGrid
import com.wallkraft.app.util.toUserMessage

@Composable
fun BrowseScreen(
    container: AppContainer,
    onOpenWallpaper: (String) -> Unit,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
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

    Scaffold(
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
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShapeDp,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = { searchText = ""; viewModel.search("") }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.search_clear))
                            }
                        }
                    },
                    // Search boxes shouldn't autocorrect — users type tags and
                    // short keywords where a "correction" silently changes the
                    // query (and Gboard loves to append punctuation).
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = false,
                        imeAction = ImeAction.Search,
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboard?.hide()
                            viewModel.search(searchText)
                        },
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                )
                IconButton(onClick = { showFilters = true }) {
                    Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.filter_title))
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
