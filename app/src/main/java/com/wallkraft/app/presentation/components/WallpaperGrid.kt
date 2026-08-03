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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.domain.model.Wallpaper
import kotlinx.coroutines.flow.distinctUntilChanged

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
    onOpen: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    footer: @Composable () -> Unit = {},
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
) {
    val gridState = state

    LaunchedEffect(gridState) {
        snapshotFlow {
            val info = gridState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to info.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (lastVisible, total) ->
                if (total > 0 && lastVisible >= total - 6) onLoadMore()
            }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = gridState,
        contentPadding = PaddingValues(KraftSpacing.ScreenEdge),
        horizontalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
        verticalItemSpacing = KraftSpacing.Spacing8,
        modifier = modifier.fillMaxSize(),
    ) {
        items(wallpapers, key = { it.id }) { wallpaper ->
            WallpaperCard(
                wallpaper = wallpaper,
                onClick = { onOpen(wallpaper.id) },
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
