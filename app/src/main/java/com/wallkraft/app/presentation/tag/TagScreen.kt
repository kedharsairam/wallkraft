package com.wallkraft.app.presentation.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wallkraft.app.AppContainer
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.presentation.components.EmptyState
import com.wallkraft.app.presentation.components.ErrorState
import com.wallkraft.app.presentation.components.GridAppendFooter
import com.wallkraft.app.presentation.components.RateLimitBanner
import com.wallkraft.app.presentation.components.ShimmerGrid
import com.wallkraft.app.presentation.components.WallpaperGrid
import com.wallkraft.app.util.WallpaperActions
import com.wallkraft.app.util.toUserMessage

/**
 * A full-screen grid of wallpapers sharing a single tag. Reuses [WallpaperGrid]
 * (infinite scroll + pagination) and a [TagViewModel] that fixes the query to
 * the tag. The bottom nav bar is hidden (handled by the NavHost).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagScreen(
    container: AppContainer,
    tag: String,
    onBack: () -> Unit,
    onOpenWallpaper: (Wallpaper) -> Unit,
    navBarPadding: androidx.compose.ui.unit.Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val viewModel: TagViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                TagViewModel(
                    tag = tag,
                    repository = container.wallpaperRepository,
                    settingsRepository = container.settings,
                    errorMessage = { e -> e.toUserMessage(container.resources) },
                )
            }
        },
    )
    val uiState by viewModel.uiState.collectAsState()
    val gridState = rememberLazyStaggeredGridState()
    var downloadedIds by remember { mutableStateOf(emptySet<String>()) }

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        downloadedIds = WallpaperActions.downloadedIds(context)
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top bar — back + tag title.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = KraftSpacing.Spacing4, vertical = KraftSpacing.Spacing4),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(
                text = "#$tag",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (uiState.rateLimited) {
                    RateLimitBanner(modifier = Modifier.padding(horizontal = KraftSpacing.Spacing16))
                    Spacer(Modifier.height(KraftSpacing.Spacing8))
                }

                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize().clipToBounds(),
                ) {
                    when {
                        uiState.isInitialLoading -> ShimmerGrid()
                        uiState.error != null && uiState.wallpapers.isEmpty() ->
                            ErrorState(message = uiState.error ?: "", onRetry = viewModel::retry)
                        uiState.wallpapers.isEmpty() ->
                            EmptyState(
                                title = stringResource(R.string.no_results_title),
                                message = stringResource(R.string.no_results_hint_query, tag),
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