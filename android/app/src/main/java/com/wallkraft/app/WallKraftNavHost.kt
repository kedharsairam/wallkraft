package com.wallkraft.app

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wallkraft.app.core.design.KraftColors
import com.wallkraft.app.core.design.KraftIconSize
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.core.design.KraftTopBar
import com.wallkraft.app.core.design.KraftTypeScale
import com.wallkraft.app.presentation.browse.BrowseScreen
import com.wallkraft.app.presentation.browse.BrowseSearchState
import com.wallkraft.app.presentation.components.SearchFilterBar
import com.wallkraft.app.presentation.detail.DetailScreen
import com.wallkraft.app.presentation.favorites.FavoritesScreen
import com.wallkraft.app.presentation.favorites.FavoritesTopBarState
import com.wallkraft.app.presentation.settings.SettingsScreen

object Routes {
    const val BROWSE = "browse?query={query}&title={title}"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{id}?thumb={thumb}&path={path}"

    fun browse(query: String = "", title: String = "") =
        "browse?query=${Uri.encode(query)}&title=${Uri.encode(title)}"
    fun detail(id: String, thumb: String? = null, path: String? = null) =
        "detail/$id?thumb=${Uri.encode(thumb ?: "")}&path=${Uri.encode(path ?: "")}"
}

private data class Tab(
    val route: String,
    @androidx.annotation.StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

// Outlined = default state, Filled = selected state — standard tab bar convention.
private val tabs = listOf(
    Tab(Routes.BROWSE, R.string.tab_browse, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    Tab(Routes.FAVORITES, R.string.tab_favorites, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    Tab(Routes.SETTINGS, R.string.tab_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun WallKraftNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val browseGridState = rememberLazyStaggeredGridState()
    val favoritesGridState = rememberLazyStaggeredGridState()

    val isDetail = currentDestination?.route == Routes.DETAIL
    val isBrowse = currentDestination?.hierarchy?.any {
        it.route?.startsWith("browse") == true
    } == true
    val isFavorites = currentDestination?.route == Routes.FAVORITES
    val isSettings = currentDestination?.route == Routes.SETTINGS

    // Shared state holders — outside SharedTransitionLayout so the top bar
    // is never eclipsed by the shared element overlay.
    val browseSearchState = remember { BrowseSearchState() }
    val favoritesTopBarState = remember { FavoritesTopBarState() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // ── Top bar (outside SharedTransitionLayout) ──────────────
            // Always rendered at the same height so innerPadding.top is
            // stable — no layout jumps when navigating to/from Detail.
            val density = LocalDensity.current
            val statusBarPadding = WindowInsets.statusBars
                .asPaddingValues(density)
                .calculateTopPadding()
            val topBarHeight = statusBarPadding + KraftSpacing.Spacing8 +
                KraftSpacing.TopBarHeight + KraftSpacing.Spacing8 + 1.dp

            Box(modifier = Modifier.heightIn(min = topBarHeight)) {
                when {
                    isBrowse -> SearchFilterBar(
                        query = browseSearchState.query,
                        onQueryChange = {
                            browseSearchState.query = it
                            browseSearchState.titleActive = false
                        },
                        onSearch = { text -> browseSearchState.onSearch?.invoke(text) },
                        filters = browseSearchState.filters,
                        onFiltersChange = { browseSearchState.onFiltersChange?.invoke(it) },
                        hasApiKey = browseSearchState.hasApiKey,
                    )
                    isFavorites -> {
                        val title = if (favoritesTopBarState.selectionMode) {
                            pluralStringResource(
                                R.plurals.selected_count,
                                favoritesTopBarState.selectedCount,
                                favoritesTopBarState.selectedCount,
                            )
                        } else {
                            stringResource(R.string.favorites_title)
                        }
                        KraftTopBar(
                            title = title,
                            navigationIcon = if (favoritesTopBarState.selectionMode) {
                                {
                                    IconButton(onClick = { favoritesTopBarState.onCancelSelection() }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = stringResource(R.string.cancel),
                                        )
                                    }
                                }
                            } else null,
                            actions = {
                                // Selection mode actions
                                AnimatedVisibility(
                                    visible = favoritesTopBarState.selectionMode,
                                    enter = fadeIn(tween(220)) + androidx.compose.animation.scaleIn(tween(220), initialScale = 0.8f),
                                    exit = fadeOut(tween(180)) + androidx.compose.animation.scaleOut(tween(180), targetScale = 0.8f),
                                ) {
                                    Row {
                                        val allSelected = favoritesTopBarState.totalFavorites > 0 &&
                                            favoritesTopBarState.selectedCount == favoritesTopBarState.totalFavorites
                                        val haptic = LocalHapticFeedback.current
                                        TextButton(
                                            onClick = {
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                favoritesTopBarState.onToggleSelectAll()
                                            },
                                        ) {
                                            Text(
                                                stringResource(
                                                    if (allSelected) R.string.deselect_all else R.string.select_all,
                                                ),
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                favoritesTopBarState.onDeleteSelected()
                                            },
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = stringResource(R.string.delete),
                                                tint = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    }
                                }
                                // Non-selection mode: "Select" button
                                AnimatedVisibility(
                                    visible = !favoritesTopBarState.selectionMode && favoritesTopBarState.totalFavorites > 0,
                                    enter = fadeIn(tween(220)),
                                    exit = fadeOut(tween(180)),
                                ) {
                                    TextButton(onClick = {
                                        favoritesTopBarState.onEnterSelectionMode()
                                    }) {
                                        Text(stringResource(R.string.select))
                                    }
                                }
                            },
                        )
                    }
                    isSettings -> KraftTopBar(
                        title = stringResource(R.string.settings_title),
                    )
                }
            }
        },
        bottomBar = {
            if (!isDetail) {
                // ── Tab Bar ───────────────────────────────────────────
                // Solid surface background, thin top separator, no indicator pill.
                // Icons: 25dp, labels: 10sp, active = primary, inactive = #8E8E93.
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        thickness = 0.5.dp,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                    tabs.forEach { tab ->
                        val selected =
                            currentDestination?.hierarchy?.any {
                                if (tab.route == Routes.BROWSE) it.route?.startsWith("browse") == true
                                else it.route == tab.route
                            } == true
                        HigTabItem(
                            tab = tab,
                            selected = selected,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val route = if (tab.route == Routes.BROWSE) Routes.browse() else tab.route
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                    }
                }
            }
        },
    ) { innerPadding ->
        SharedTransitionLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.BROWSE,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(
                    route = Routes.BROWSE,
                    arguments = listOf(
                        navArgument("query") { type = NavType.StringType; defaultValue = "" },
                        navArgument("title") { type = NavType.StringType; defaultValue = "" },
                    ),
                    enterTransition = { fadeIn(tween(220)) },
                    exitTransition = { fadeOut(tween(220)) },
                ) { entry ->
                    val query = entry.arguments?.getString("query").orEmpty()
                    val title = entry.arguments?.getString("title").orEmpty()
                    BrowseScreen(
                        container = container,
                        onOpenWallpaper = { w -> navController.navigate(Routes.detail(w.id, w.thumbnail, w.path)) },
                        gridState = if (query.isBlank()) browseGridState else null,
                        navBarPadding = innerPadding.calculateBottomPadding(),
                        initialQuery = query,
                        title = title,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable,
                        searchState = browseSearchState,
                    )
                }
                composable(
                    Routes.FAVORITES,
                    enterTransition = { fadeIn(tween(220)) },
                    exitTransition = { fadeOut(tween(220)) },
                ) {
                    FavoritesScreen(
                        container = container,
                        onOpenWallpaper = { w -> navController.navigate(Routes.detail(w.id, w.thumbnail, w.path)) },
                        gridState = favoritesGridState,
                        navBarPadding = innerPadding.calculateBottomPadding(),
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable,
                        topBarState = favoritesTopBarState,
                    )
                }
                composable(
                    Routes.SETTINGS,
                    enterTransition = { fadeIn(tween(220)) },
                    exitTransition = { fadeOut(tween(220)) },
                ) {
                    SettingsScreen(
                        container = container,
                        navBarPadding = innerPadding.calculateBottomPadding(),
                    )
                }
                composable(
                    route = Routes.DETAIL,
                    arguments = listOf(
                        navArgument("id") { type = NavType.StringType },
                        navArgument("thumb") { type = NavType.StringType; defaultValue = "" },
                        navArgument("path") { type = NavType.StringType; defaultValue = "" },
                    ),
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { fadeOut(spring(dampingRatio = 0.7f, stiffness = 400f)) },
                ) { entry ->
                    DetailScreen(
                        container = container,
                        wallpaperId = entry.arguments?.getString("id").orEmpty(),
                        previewThumb = entry.arguments?.getString("thumb").orEmpty(),
                        previewPath = entry.arguments?.getString("path").orEmpty(),
                        onBack = { navController.popBackStack() },
                        onTagClick = { tag -> navController.navigate(Routes.browse(tag)) },
                        onUploaderClick = { username ->
                            navController.navigate(Routes.browse("@$username", title = username))
                        },
                        navBarPadding = 0.dp,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable,
                    )
                }
            }
        }
    }
}

/**
 * Single tab item following standard design conventions:
 * - 25dp icon
 * - 10sp label (SF Pro Text weight)
 * - Active: primary color, Inactive: #8E8E93
 * - No indicator pill — just color change
 */
@Composable
private fun HigTabItem(
    tab: Tab,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else KraftColors.TabBarInactive
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onClick()
                },
            )
            .padding(top = KraftSpacing.Spacing12, bottom = KraftSpacing.Spacing4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
            contentDescription = stringResource(tab.labelRes),
            tint = tint,
            modifier = Modifier.size(KraftIconSize.TabBar),
        )
        Spacer(Modifier.height(KraftSpacing.Spacing2))
        Text(
            text = stringResource(tab.labelRes),
            fontSize = KraftTypeScale.Caption2,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TextButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    androidx.compose.material3.TextButton(onClick = onClick) { content() }
}
