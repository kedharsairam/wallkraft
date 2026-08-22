package com.wallkraft.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import com.wallkraft.app.core.design.KraftConstants
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
 */
@Composable
fun WallpaperGrid(
    wallpapers: List<Wallpaper>,
    onOpen: (Wallpaper) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    footer: @Composable () -> Unit = {},
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    downloadedIds: Set<String> = emptySet(),
    // When false (data saver on), tapping a tile does NOT prefetch the
    // full-res image — the detail screen defers it until the user zooms.
    prefetchFullRes: Boolean = true,
    onLongClick: ((Wallpaper) -> Unit)? = null,
    selectionMode: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onToggleSelect: ((Wallpaper) -> Unit)? = null,
) {
    val gridState = state

    val context = LocalContext.current
    val gridImageLoader = GridImageLoader.get() ?: context.imageLoader

    // Lower fling friction than the platform default so a swipe glides further
    // and coasts to a stop — the "smooth, glides for longer" feel of the
    // reference app instead of the default's quick, grippy stop.
    val flingBehavior = remember(gridState) { SmoothFlingBehavior() }

    LaunchedEffect(gridState) {
        snapshotFlow {
            val info = gridState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to info.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (lastVisible, total) ->
                // Preload the next page well before the end (20 items out) so
                // the slow Wallhaven API has time to respond before the user
                // actually reaches the bottom — no visible wait at the end.
                if (total > 0 && lastVisible >= total - KraftConstants.GridPrefetchThreshold) onLoadMore()
            }
    }

    // Prefetch a few thumbnails ahead of the viewport so tiles are already in
    // the memory cache when they scroll into view. Debounced so it only runs
    // after the user pauses scrolling — prefetching on every scroll frame would
    // flood Coil's shared queue and make the tiles that actually need to load
    // wait behind the prefetch jobs (slower loading).
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
                    // enqueue with no target = prefetch into the cache without
                    // drawing, so the tile is ready when it scrolls into view.
                    gridImageLoader.enqueue(
                        ImageRequest.Builder(context).data(url).build(),
                    )
                }
            }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
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
            WallpaperCard(
                wallpaper = wallpaper,
                onClick = {
                    if (selectionMode) {
                        onToggleSelect?.invoke(wallpaper)
                    } else {
                        // Warm the full-res image before the detail screen opens so
                        // it appears instantly (the detail screen already seeds a
                        // preview from the thumbnail; this gets the full-res into
                        // the cache ahead of time). enqueue with no target = fetch
                        // into the cache without drawing. Skipped in data saver
                        // mode — the detail screen defers the download instead.
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
            .height(56.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.height(24.dp).width(24.dp))
    }
}
