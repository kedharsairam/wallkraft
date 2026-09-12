package com.wallkraft.app

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.FolderDelete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
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
import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.core.design.KraftIconSize
import com.wallkraft.app.core.design.KraftRadius
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.core.design.KraftTopBar
import com.wallkraft.app.core.design.KraftTypeScale
import com.wallkraft.app.core.utils.rememberReduceMotion
import com.wallkraft.app.data.prefs.RotationStore
import com.wallkraft.app.di.AppDependenciesViewModel
import com.wallkraft.app.presentation.browse.BrowseScreen
import com.wallkraft.app.presentation.browse.BrowseSearchState
import com.wallkraft.app.presentation.components.RotationTimingWelcome
import com.wallkraft.app.presentation.components.SearchFilterBar
import com.wallkraft.app.presentation.components.glass.GlassBox
import com.wallkraft.app.presentation.components.glass.GlassContainer
import com.wallkraft.app.presentation.components.glass.GlassContainerWithHidden
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

private data class Tab(
    val route: String,
    @androidx.annotation.StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val tabs = listOf(
    Tab(Routes.BROWSE, R.string.tab_browse, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    Tab(Routes.FAVORITES, R.string.tab_favorites, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    Tab(Routes.SETTINGS, R.string.tab_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun WallKraftNavHost() {
    val deps: AppDependenciesViewModel = hiltViewModel()
    WallKraftNavHostImpl(rotationStore = deps.rotationStore)
}

@Deprecated("Use Hilt version — container will be removed")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun WallKraftNavHost(container: AppContainer) {
    WallKraftNavHostImpl(rotationStore = container.rotation)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun WallKraftNavHostImpl(rotationStore: RotationStore) {
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
        KraftSpacing.TopBarHeight + KraftSpacing.Spacing8 + KraftSpacing.BorderWidth

    val timingSeen by rotationStore.timingWelcomeSeen.collectAsState(initial = true)
    val hostScope = rememberCoroutineScope()
    if (!timingSeen) {
        RotationTimingWelcome(
            onDone = { hostScope.launch { rotationStore.markTimingWelcomeSeen() } },
        )
    }

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
                    enterTransition = { if (reduceMotion) androidx.compose.animation.EnterTransition.None else fadeIn(tween(220)) },
                    exitTransition = { if (reduceMotion) androidx.compose.animation.ExitTransition.None else fadeOut(tween(220)) },
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
                        sharedTransitionScope = null,
                        animatedVisibilityScope = null,
                        searchState = browseSearchState,
                    )
                }
                composable(
                    Routes.FAVORITES,
                    enterTransition = { if (reduceMotion) androidx.compose.animation.EnterTransition.None else fadeIn(tween(220)) },
                    exitTransition = { if (reduceMotion) androidx.compose.animation.ExitTransition.None else fadeOut(tween(220)) },
                ) {
                    FavoritesScreen(
                        onOpenWallpaper = { w -> navController.navigate(Routes.detail(w.id, w.thumbnail, w.path)) },
                        gridState = favoritesGridState,
                        navBarPadding = innerPadding.calculateBottomPadding(),
                        topInset = topInset,
                        sharedTransitionScope = null,
                        animatedVisibilityScope = null,
                        topBarState = favoritesTopBarState,
                    )
                }
                composable(
                    Routes.SETTINGS,
                    enterTransition = { if (reduceMotion) androidx.compose.animation.EnterTransition.None else fadeIn(tween(220)) },
                    exitTransition = { if (reduceMotion) androidx.compose.animation.ExitTransition.None else fadeOut(tween(220)) },
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
                    enterTransition = { if (reduceMotion) androidx.compose.animation.EnterTransition.None else fadeIn(tween(220)) + androidx.compose.animation.scaleIn(tween(220), initialScale = 0.96f) },
                    exitTransition = { if (reduceMotion) androidx.compose.animation.ExitTransition.None else fadeOut(tween(180)) },
                    popEnterTransition = { if (reduceMotion) androidx.compose.animation.EnterTransition.None else fadeIn(tween(220)) },
                    popExitTransition = { if (reduceMotion) androidx.compose.animation.ExitTransition.None else fadeOut(tween(180)) },
                ) { entry ->
                    DetailScreen(
                        wallpaperId = entry.arguments?.getString("id").orEmpty(),
                        previewThumb = entry.arguments?.getString("thumb").orEmpty(),
                        previewPath = entry.arguments?.getString("path").orEmpty(),
                        onBack = { navController.popBackStack() },
                        onTagClick = { tag -> navController.navigate(Routes.browse(tag)) },
                        onUploaderClick = { username ->
                            navController.navigate(Routes.browse("@$username", title = username))
                        },
                        navBarPadding = 0.dp,
                        sharedTransitionScope = null,
                        animatedVisibilityScope = null,
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
                                        AnimatedVisibility(
                                            visible = favoritesTopBarState.selectionMode,
                                            enter = if (reduceMotion) fadeIn(tween(220)) else fadeIn(tween(220)) + androidx.compose.animation.scaleIn(tween(220), initialScale = 0.8f),
                                            exit = if (reduceMotion) fadeOut(tween(180)) else fadeOut(tween(180)) + androidx.compose.animation.scaleOut(tween(180), targetScale = 0.8f),
                                        ) {
                                            Row {
                                                val haptic = LocalHapticFeedback.current
                                                TextButton(
                                                    onClick = {
                                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                        favoritesTopBarState.onToggleSelectAll()
                                                    },
                                                ) {
                                                    Text(
                                                        stringResource(
                                                            if (favoritesTopBarState.allVisibleSelected) R.string.deselect_all else R.string.select_all,
                                                        ),
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                        favoritesTopBarState.onAddToCollection()
                                                    },
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.CreateNewFolder,
                                                        contentDescription = stringResource(R.string.add_to_collection),
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                        if (favoritesTopBarState.isInCollection) {
                                                            favoritesTopBarState.onRemoveFromCollection?.invoke()
                                                        } else {
                                                            favoritesTopBarState.onDeleteSelected()
                                                        }
                                                    },
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Delete,
                                                        contentDescription = if (favoritesTopBarState.isInCollection) stringResource(R.string.remove_from_collection) else stringResource(R.string.delete),
                                                        tint = MaterialTheme.colorScheme.error,
                                                    )
                                                }
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
                            tabs = tabs,
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

private val GlassShape = CircleShape

@Composable
private fun GlassTabBar(
    tabs: List<Tab>,
    currentDestination: NavDestination?,
    onTabClick: (Tab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = KraftConstants.GlassTabOuterAlpha), GlassShape)
            .background(Color.White.copy(alpha = KraftConstants.GlassTabInnerAlpha), GlassShape)
            .background(KraftColors.SurfaceTertiary.copy(alpha = KraftConstants.GlassTabMidAlpha), GlassShape)
            .background(KraftColors.SurfaceSecondary.copy(alpha = KraftConstants.GlassTabHighlightAlpha), GlassShape)
            .padding(horizontal = KraftSpacing.Spacing4, vertical = KraftSpacing.Spacing4),
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
                onClick = { onTabClick(tab) },
            )
        }
    }
}

@Composable
private fun HigTabItem(
    tab: Tab,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else KraftColors.TextPrimary
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "tabPress",
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(KraftRadius.Pill))
            .background(
                if (selected) KraftColors.Surface
                else Color.Transparent,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
            fontWeight = FontWeight.SemiBold,
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
