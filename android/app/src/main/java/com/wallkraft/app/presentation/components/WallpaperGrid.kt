package com.wallkraft.app.presentation.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.imageLoader
import coil3.request.ImageRequest
import com.wallkraft.app.core.cache.GridImageLoader
import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.core.design.KraftIconSize
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.domain.model.Wallpaper
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

/** How many tiles ahead of the viewport to prefetch into the image cache. */
private const val PREFETCH_AHEAD = KraftConstants.GridPrefetchAhead

/**
 * The staggered wallpaper grid with infinite-scroll pagination.
 * Calls [onLoadMore] when the user scrolls near the end.
 *
 * [state] can be hoisted by the caller so the scroll position survives tab
 * switches (each screen must pass its own).
 *
 * [sharedTransitionScope] and [animatedVisibilityScope] enable the
 * container-transform shared element transition from grid tile to detail
 * screen. Pass null to disable.
 */
@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun WallpaperGrid(
    wallpapers: List<Wallpaper>,
    onOpen: (Wallpaper) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    footer: @Composable () -> Unit = {},
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    downloadedIds: Set<String> = emptySet(),
    prefetchFullRes: Boolean = true,
    onLongClick: ((Wallpaper) -> Unit)? = null,
    selectionMode: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onToggleSelect: ((Wallpaper) -> Unit)? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val gridState = state

    val context = LocalContext.current
    val gridImageLoader = GridImageLoader.get() ?: context.imageLoader

    val flingBehavior = remember(gridState) { SmoothFlingBehavior() }

    LaunchedEffect(gridState) {
        snapshotFlow {
            val info = gridState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to info.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (lastVisible, total) ->
                if (total > 0 && lastVisible >= total - KraftConstants.GridPrefetchThreshold) onLoadMore()
            }
    }

    @OptIn(FlowPreview::class)
    LaunchedEffect(gridState, wallpapers) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        }
            .distinctUntilChanged()
            .debounce(KraftConstants.GridPrefetchDebounceMs)
            .collect { lastVisible ->
                if (lastVisible < 0) return@collect
                val start = lastVisible + 1
                val end = minOf(start + PREFETCH_AHEAD, wallpapers.size)
                for (i in start until end) {
                    val url = wallpapers[i].thumbnail ?: continue
                    gridImageLoader.enqueue(
                        ImageRequest.Builder(context).data(url).build(),
                    )
                }
            }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(KraftSpacing.GridTileMin),
        state = gridState,
        contentPadding = PaddingValues(
            start = KraftSpacing.ScreenEdge,
            end = KraftSpacing.ScreenEdge,
            top = KraftSpacing.Spacing8,
            bottom = KraftSpacing.Spacing8,
        ),
        horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
        verticalItemSpacing = KraftSpacing.Spacing8,
        flingBehavior = flingBehavior,
        modifier = modifier.fillMaxSize(),
    ) {
        items(wallpapers, key = { it.id }) { wallpaper ->
            // Build the shared element modifier inside the SharedTransitionScope
            // so it can participate in the container-transform animation.
            // The official API requires both SharedContentState AND
            // animatedVisibilityScope.
            val sharedElementModifier: Modifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    Modifier.sharedElement(
                        state = rememberSharedContentState(key = wallpaper.id),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> tween(220) },
                    )
                }
            } else {
                Modifier
            }

            WallpaperCard(
                wallpaper = wallpaper,
                onClick = {
                    if (selectionMode) {
                        onToggleSelect?.invoke(wallpaper)
                    } else {
                        if (prefetchFullRes) {
                            val fullUrl = wallpaper.path ?: wallpaper.thumbnail
                            if (fullUrl != null) {
                                gridImageLoader.enqueue(
                                    ImageRequest.Builder(context).data(fullUrl).build(),
                                )
                            }
                        }
                        onOpen(wallpaper)
                    }
                },
                onLongClick = onLongClick?.let { longClick ->
                    { longClick(wallpaper) }
                },
                downloadedIds = downloadedIds,
                selectionMode = selectionMode,
                selected = wallpaper.id in selectedIds,
                sharedElementModifier = sharedElementModifier,
            )
        }
        item(span = StaggeredGridItemSpan.FullLine) { footer() }
    }
}

/** Appending spinner shown at the bottom of the grid during pagination. */
@Composable
fun GridAppendFooter() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(KraftSpacing.Spacing48),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(KraftIconSize.Large))
    }
}
