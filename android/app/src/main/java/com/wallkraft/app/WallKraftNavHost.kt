package com.wallkraft.app

import android.net.Uri
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    const val BROWSE = "browse?query={query}&title={title}"
    const val FAVORITES = "favorites"
    const val DOWNLOADS = "downloads"
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

private val tabs = listOf(
    Tab(Routes.BROWSE, R.string.tab_browse, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    Tab(Routes.FAVORITES, R.string.tab_favorites, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    Tab(Routes.DOWNLOADS, R.string.tab_downloads, Icons.Filled.Download, Icons.Outlined.Download),
    Tab(Routes.SETTINGS, R.string.tab_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun WallKraftNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isDetail = currentDestination?.route == Routes.DETAIL

    val hideBottomBar = isDetail

    val browseGridState = rememberLazyStaggeredGridState()
    val favoritesGridState = rememberLazyStaggeredGridState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!hideBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.height(90.dp),
                ) {
                    tabs.forEach { tab ->
                        val selected =
                            currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                val route = if (tab.route == Routes.BROWSE) Routes.browse() else tab.route
                                if (isDetail) {
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
                                    contentDescription = stringResource(tab.labelRes),
                                    modifier = Modifier.size(32.dp),
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = Color.Transparent,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        val navBarPadding = innerPadding.calculateBottomPadding()

        SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
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
                ) { entry ->
                    val query = entry.arguments?.getString("query").orEmpty()
                    val title = entry.arguments?.getString("title").orEmpty()
                    BrowseScreen(
                        container = container,
                        onOpenWallpaper = { w -> navController.navigate(Routes.detail(w.id, w.thumbnail, w.path)) },
                        gridState = if (query.isBlank()) browseGridState else null,
                        navBarPadding = navBarPadding,
                        initialQuery = query,
                        title = title,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable,
                    )
                }
                composable(Routes.FAVORITES) {
                    FavoritesScreen(
                        container = container,
                        onOpenWallpaper = { w -> navController.navigate(Routes.detail(w.id, w.thumbnail, w.path)) },
                        gridState = favoritesGridState,
                        navBarPadding = navBarPadding,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable,
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
                    // No enterTransition — the black background fades in
                    // manually inside DetailContent, timed to match the shared
                    // element's 220ms bounds animation. fadeIn/fadeOut would
                    // make the entire screen (including background) snap to
                    // visible instantly, which defeats the smooth effect.
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    // popExit: fade out non-shared content (background, chrome)
                    // over 220ms. The shared element handles the image return
                    // animation independently — it overrides the alpha for the
                    // shared image during the transition.
                    popExitTransition = { fadeOut(tween(220)) },
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
                        navBarPadding = navBarPadding,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable,
                    )
                }
            }
        }
    }
}
