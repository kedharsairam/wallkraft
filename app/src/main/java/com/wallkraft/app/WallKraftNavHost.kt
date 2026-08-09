package com.wallkraft.app

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.presentation.browse.BrowseScreen
import com.wallkraft.app.presentation.detail.DetailScreen
import com.wallkraft.app.presentation.downloads.DownloadsScreen
import com.wallkraft.app.presentation.favorites.FavoritesScreen
import com.wallkraft.app.presentation.settings.SettingsScreen

object Routes {
    const val BROWSE = "browse?query={query}"
    const val FAVORITES = "favorites"
    const val DOWNLOADS = "downloads"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{id}?thumb={thumb}&path={path}"

    fun browse(query: String = "") = "browse?query=${Uri.encode(query)}"
    fun detail(id: String, thumb: String? = null, path: String? = null) =
        "detail/$id?thumb=${Uri.encode(thumb ?: "")}&path=${Uri.encode(path ?: "")}"
}

private data class Tab(
    val route: String,
    @androidx.annotation.StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val tabs = listOf(
    Tab(Routes.BROWSE, R.string.tab_browse, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    Tab(Routes.FAVORITES, R.string.tab_favorites, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    Tab(Routes.DOWNLOADS, R.string.tab_downloads, Icons.Filled.Download, Icons.Outlined.Download),
    Tab(Routes.SETTINGS, R.string.tab_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallKraftNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isDetail = currentDestination?.route == Routes.DETAIL

    // The detail screen reports its zoom state up here so the bottom bar can
    // hide while the user is zoomed in — giving a clean, edge-to-edge view.
    var detailZoomed by remember { mutableStateOf(false) }
    val hideBottomBar = isDetail && detailZoomed

    // Reset the zoom flag whenever the user leaves the detail screen, so
    // returning to another image later starts un-zoomed with the bar visible.
    LaunchedEffect(isDetail) {
        if (!isDetail) detailZoomed = false
    }

    val browseGridState = rememberLazyStaggeredGridState()
    val favoritesGridState = rememberLazyStaggeredGridState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!hideBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    modifier = Modifier.height(88.dp),
                ) {
                    tabs.forEach { tab ->
                        val selected =
                            currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                // The Browse route carries an optional query; the
                                // tab always navigates to the plain (no-query) version.
                                val route = if (tab.route == Routes.BROWSE) Routes.browse() else tab.route
                                if (isDetail) {
                                    // We're on the pushed detail screen. Navigate to
                                    // the tab WITHOUT popping the back stack, so the
                                    // current image stays beneath and back returns to it.
                                    navController.navigate(route) {
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = null,
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(tab.labelRes),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.background,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        // Only pass bottom padding (nav bar height) to screens.
        // Top padding is handled by each screen's own statusBarsPadding().
        val navBarPadding = innerPadding.calculateBottomPadding()

        NavHost(
            navController = navController,
            startDestination = Routes.BROWSE,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(
                route = Routes.BROWSE,
                arguments = listOf(navArgument("query") { type = NavType.StringType; defaultValue = "" }),
            ) { entry ->
                val query = entry.arguments?.getString("query").orEmpty()
                BrowseScreen(
                    container = container,
                    onOpenWallpaper = { w -> navController.navigate(Routes.detail(w.id, w.thumbnail, w.path)) },
                    // The tab hoists its grid state so it survives tab switches;
                    // a tag-as-browse entry passes null and gets its own state.
                    gridState = if (query.isBlank()) browseGridState else null,
                    navBarPadding = navBarPadding,
                    initialQuery = query,
                )
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(
                    container = container,
                    onOpenWallpaper = { w -> navController.navigate(Routes.detail(w.id, w.thumbnail, w.path)) },
                    gridState = favoritesGridState,
                    navBarPadding = navBarPadding,
                )
            }
            composable(Routes.DOWNLOADS) {
                DownloadsScreen(
                    onOpenWallpaper = { w -> navController.navigate(Routes.detail(w.id, w.thumbnail, w.path)) },
                    navBarPadding = navBarPadding,
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(container = container, navBarPadding = navBarPadding)
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("thumb") { type = NavType.StringType; defaultValue = "" },
                    navArgument("path") { type = NavType.StringType; defaultValue = "" },
                ),
                // A single, symmetric transition for both opening and closing: fade + a
                // subtle scale, with the SAME duration and easing in every
                // direction — so the detail opens and closes identically
                // (mirrored), never feeling like two different animations.
                enterTransition = {
                    fadeIn(tween(250)) + scaleIn(initialScale = 0.96f, animationSpec = tween(250))
                },
                exitTransition = {
                    fadeOut(tween(250)) + scaleOut(targetScale = 0.96f, animationSpec = tween(250))
                },
                popEnterTransition = {
                    fadeIn(tween(250)) + scaleIn(initialScale = 0.96f, animationSpec = tween(250))
                },
                popExitTransition = {
                    fadeOut(tween(250)) + scaleOut(targetScale = 0.96f, animationSpec = tween(250))
                },
            ) { entry ->
                DetailScreen(
                    container = container,
                    wallpaperId = entry.arguments?.getString("id").orEmpty(),
                    previewThumb = entry.arguments?.getString("thumb").orEmpty(),
                    previewPath = entry.arguments?.getString("path").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onTagClick = { tag -> navController.navigate(Routes.browse(tag)) },
                    onZoomChanged = { zoomed -> detailZoomed = zoomed },
                    navBarPadding = navBarPadding,
                )
            }
        }
    }
}