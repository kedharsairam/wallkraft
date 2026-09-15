package com.wallkraft.app

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
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.hilt.navigation.compose.hiltViewModel
import com.wallkraft.app.core.design.KraftColors
import com.wallkraft.app.core.design.KraftConstants
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
import com.wallkraft.app.presentation.history.HistoryScreen
import com.wallkraft.app.presentation.settings.SettingsScreen
import com.wallkraft.app.navigation.Browse
import com.wallkraft.app.navigation.Detail
import com.wallkraft.app.navigation.Favorites
import com.wallkraft.app.navigation.History
import com.wallkraft.app.navigation.Settings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun WallKraftNavHost(initialDestination: String? = null) {
    val viewModel: NavHostViewModel = hiltViewModel()
    WallKraftNavHostImpl(rotationStore = viewModel.rotationStore, initialDestination = initialDestination)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun WallKraftNavHostImpl(
    rotationStore: RotationSettingsStore,
    initialDestination: String? = null,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val browseGridState = rememberLazyStaggeredGridState()
    val favoritesGridState = rememberLazyStaggeredGridState()

    val isDetail = currentDestination?.hierarchy?.any { it.hasRoute<Detail>() } == true
    val isBrowse = currentDestination?.hierarchy?.any { it.hasRoute<Browse>() } == true
    val isFavorites = currentDestination?.hierarchy?.any { it.hasRoute<Favorites>() } == true
    val isSettings = currentDestination?.hierarchy?.any { it.hasRoute<Settings>() } == true

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
                    val startDestination = when (initialDestination) {
                        "favorites" -> Favorites
                        else -> Browse()
                    }

                    // region Navigation graph
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        // region Browse routes
                        composable<Browse>(
                            enterTransition = {
                                if (reduceMotion) androidx.compose.animation.EnterTransition.None
                                else fadeIn(tween(220))
                            },
                            exitTransition = {
                                if (reduceMotion) androidx.compose.animation.ExitTransition.None
                                else fadeOut(tween(220))
                            },
                        ) { entry ->
                            val args = entry.toRoute<Browse>()
                            val query = args.query
                            val title = args.title
                            BrowseScreen(
                                onOpenWallpaper = { w ->
                                    navController.navigate(Detail(id = w.id, thumb = w.thumbnail.orEmpty(), path = w.path))
                                },
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
                        composable<Favorites>(
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
                                onOpenWallpaper = { w ->
                                    navController.navigate(Detail(id = w.id, thumb = w.thumbnail.orEmpty(), path = w.path))
                                },
                                onNavigateToBrowse = {
                                    navController.navigate(Browse()) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                    }
                                },
                                gridState = favoritesGridState,
                                navBarPadding = innerPadding.calculateBottomPadding(),
                                topInset = topInset,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = this,
                                topBarState = favoritesTopBarState,
                            )
                        }
                        // endregion Browse routes

                        // region Settings / History routes
                        composable<Settings>(
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
                                onHistoryClick = {
                                    navController.navigate(History)
                                },
                            )
                        }
                        composable<History>(
                            enterTransition = {
                                if (reduceMotion) androidx.compose.animation.EnterTransition.None
                                else fadeIn(tween(220))
                            },
                            exitTransition = {
                                if (reduceMotion) androidx.compose.animation.ExitTransition.None
                                else fadeOut(tween(220))
                            },
                        ) {
                            HistoryScreen(
                                onBack = { navController.popBackStack() },
                            )
                        }
                        // endregion Settings / History routes

                        // region Detail route
                        composable<Detail>(
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
                            val detail = entry.toRoute<Detail>()
                            DetailScreen(
                                wallpaperId = detail.id,
                                previewThumb = detail.thumb,
                                previewPath = detail.path,
                                onBack = { navController.popBackStack() },
                                onTagClick = { tag ->
                                    navController.navigate(Browse(query = tag))
                                },
                                onUploaderClick = { username ->
                                    navController.navigate(Browse(query = "@$username", title = username))
                                },
                                navBarPadding = 0.dp,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = this,
                            )
                        }
                        // endregion Detail route
                    }
                    // endregion Navigation graph
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
                        // 8dp glass elevation + 0.08 white glow are fixed glass specs.
                        elevation = 8.dp,
                        tint = KraftColors.TextPrimary.copy(alpha = KraftConstants.NavHostGlowAlpha),
                        darkness = 0.10f,
                        warpEdges = 0.22f,
                    ) {
                        GlassTabBar(
                            tabs = defaultTabs,
                            currentDestination = currentDestination,
                            onTabClick = { tab ->
                                navController.navigate(tab.destination) {
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
