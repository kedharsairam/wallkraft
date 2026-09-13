package com.wallkraft.app

import android.net.Uri
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.wallkraft.app.core.design.KraftColors
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.core.design.KraftTopBar
import com.wallkraft.app.core.utils.rememberReduceMotion
import com.wallkraft.app.data.prefs.RotationSettingsStore
import com.wallkraft.app.presentation.NavHostViewModel
import com.wallkraft.app.presentation.browse.BrowseScreen
import com.wallkraft.app.presentation.browse.BrowseSearchState
import com.wallkraft.app.presentation.components.FavoritesSelectionTopBar
import com.wallkraft.app.presentation.components.glass.GlassBox
import com.wallkraft.app.presentation.components.glass.GlassContainerWithHidden
import com.wallkraft.app.presentation.components.GlassTabBar
import com.wallkraft.app.presentation.components.WelcomeScreen
import com.wallkraft.app.presentation.components.SearchFilterBar
import com.wallkraft.app.presentation.components.defaultTabs
import com.wallkraft.app.presentation.detail.DetailScreen
import com.wallkraft.app.presentation.favorites.FavoritesScreen
import com.wallkraft.app.presentation.favorites.FavoritesTopBarState
import com.wallkraft.app.presentation.settings.SettingsScreen
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun WallKraftNavHost() {
    val viewModel: NavHostViewModel = hiltViewModel()
    WallKraftNavHostImpl(rotationStore = viewModel.rotationStore)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun WallKraftNavHostImpl(rotationStore: RotationSettingsStore) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val browseGridState = rememberLazyStaggeredGridState()
    val favoritesGridState = rememberLazyStaggeredGridState()

    val isDetail = currentDestination?.route?.contains("detail", ignoreCase = true) == true ||
        backStackEntry?.destination?.route?.contains("detail", ignoreCase = true) == true ||
        currentDestination?.hierarchy?.any { it.route?.contains("detail", ignoreCase = true) == true } == true
    val isBrowse = currentDestination?.hierarchy?.any {
        it.route?.startsWith("browse") == true
    } == true
    val isFavorites = currentDestination?.route == Routes.FAVORITES
    val isSettings = currentDestination?.route == Routes.SETTINGS

    val browseSearchState = remember { BrowseSearchState() }
    val favoritesTopBarState = remember { FavoritesTopBarState() }

    val density = LocalDensity.current
    val reduceMotion = rememberReduceMotion()
    val topInset = WindowInsets.statusBars
        .asPaddingValues(density)
        .calculateTopPadding() + KraftSpacing.Spacing8 +
        KraftSpacing.TopBarHeight + KraftSpacing.Spacing8

    val timingSeen by rotationStore.timingWelcomeSeen.collectAsState(initial = true)
    val hostScope = rememberCoroutineScope()
    if (!timingSeen) {
        WelcomeScreen(
            onDone = { hostScope.launch { rotationStore.markTimingWelcomeSeen() } },
        )
    } else {

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {},
        bottomBar = {},
    ) { innerPadding ->
        GlassContainerWithHidden(
            modifier = Modifier.fillMaxSize(),
            hidden = isDetail,
            content = {
                SharedTransitionLayout(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val sharedTransitionScope = this
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
                    enterTransition = {
                        if (reduceMotion) androidx.compose.animation.EnterTransition.None
                        else fadeIn(tween(220))
                    },
                    exitTransition = {
                        if (reduceMotion) androidx.compose.animation.ExitTransition.None
                        else fadeOut(tween(220))
                    },
                ) { entry ->
                    val query = entry.arguments?.getString("query").orEmpty()
                    val title = entry.arguments?.getString("title").orEmpty()
                    BrowseScreen(
                        onOpenWallpaper = { w -> navController.navigate(Routes.detail(w.id, w.thumbnail, w.path)) },
                        gridState = if (query.isBlank()) browseGridState else null,
                        navBarPadding = innerPadding.calculateBottomPadding(),
                        topInset = topInset,
                        initialQuery = query,
                        title = title,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this,
                        searchState = browseSearchState,
                    )
                }
                composable(
                    Routes.FAVORITES,
                    enterTransition = {
                        if (reduceMotion) androidx.compose.animation.EnterTransition.None
                        else fadeIn(tween(220))
                    },
                    exitTransition = {
                        if (reduceMotion) androidx.compose.animation.ExitTransition.None
                        else fadeOut(tween(220))
                    },
                ) {
                    FavoritesScreen(
                        onOpenWallpaper = { w -> navController.navigate(Routes.detail(w.id, w.thumbnail, w.path)) },
                        gridState = favoritesGridState,
                        navBarPadding = innerPadding.calculateBottomPadding(),
                        topInset = topInset,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this,
                        topBarState = favoritesTopBarState,
                    )
                }
                composable(
                    Routes.SETTINGS,
                    enterTransition = {
                        if (reduceMotion) androidx.compose.animation.EnterTransition.None
                        else fadeIn(tween(220))
                    },
                    exitTransition = {
                        if (reduceMotion) androidx.compose.animation.ExitTransition.None
                        else fadeOut(tween(220))
                    },
                ) {
                    SettingsScreen(
                        navBarPadding = innerPadding.calculateBottomPadding(),
                        topInset = topInset,
                    )
                }
                composable(
                    route = Routes.DETAIL,
                    arguments = listOf(
                        navArgument("id") { type = NavType.StringType },
                        navArgument("thumb") { type = NavType.StringType; defaultValue = "" },
                        navArgument("path") { type = NavType.StringType; defaultValue = "" },
                    ),
                    enterTransition = {
                        if (reduceMotion) androidx.compose.animation.EnterTransition.None
                        else fadeIn(tween(220)) +
                            androidx.compose.animation.scaleIn(
                                tween(220),
                                initialScale = 0.96f,
                            )
                    },
                    exitTransition = {
                        if (reduceMotion) androidx.compose.animation.ExitTransition.None
                        else fadeOut(tween(180))
                    },
                    popEnterTransition = {
                        if (reduceMotion) androidx.compose.animation.EnterTransition.None
                        else fadeIn(tween(220))
                    },
                    popExitTransition = {
                        if (reduceMotion) androidx.compose.animation.ExitTransition.None
                        else fadeOut(tween(180))
                    },
                ) { entry ->
                    DetailScreen(
                        wallpaperId = entry.arguments?.getString("id").orEmpty(),
                        previewThumb = entry.arguments?.getString("thumb").orEmpty(),
                        previewPath = entry.arguments?.getString("path").orEmpty(),
                        onBack = { navController.popBackStack() },
                        onTagClick = { tag ->
                            navController.navigate(Routes.browse(tag)) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                            }
                        },
                        onUploaderClick = { username ->
                            navController.navigate(Routes.browse("@$username", title = username)) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                            }
                        },
                        navBarPadding = 0.dp,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this,
                    )
                }
            }
            }
            },
            glassContent = {
                if (!isDetail) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(topInset)
                            .background(KraftColors.SurfaceSecondary),
                    ) {}
                    Box(
                        modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                    ) {
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
                                totalResults = browseSearchState.totalResults,
                                hasApiKey = browseSearchState.hasApiKey,
                                history = browseSearchState.history,
                                onClearHistory = { browseSearchState.onClearHistory?.invoke() },
                            )
                            isFavorites -> FavoritesSelectionTopBar(topBarState = favoritesTopBarState)
                            isSettings -> KraftTopBar(
                                title = stringResource(R.string.settings_title),
                            )
                        }
                    }
                    GlassBox(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(vertical = KraftSpacing.Spacing2)
                            .padding(horizontal = KraftSpacing.Spacing24)
                            .fillMaxWidth(),
                        blur = 0.95f,
                        scale = 0.12f,
                        centerDistortion = 0f,
                        shape = RoundedCornerShape(KraftRadius.Pill),
                        elevation = 8.dp,
                        tint = Color.White.copy(alpha = 0.08f),
                        darkness = 0.10f,
                        warpEdges = 0.22f,
                    ) {
                        GlassTabBar(
                            tabs = defaultTabs,
                            currentDestination = currentDestination,
                            onTabClick = { tab ->
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
            },
        )
    }
    }
}
