package com.wallkraft.app.presentation.favorites

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.wallkraft.app.AppContainer
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.data.cache.FavoriteOfflineRepair
import com.wallkraft.app.domain.model.Wallpaper
import androidx.compose.animation.ExperimentalSharedTransitionApi
import com.wallkraft.app.presentation.components.EmptyState
import com.wallkraft.app.presentation.components.WallpaperGrid
import com.wallkraft.app.domain.model.DownloadedFile
import com.wallkraft.app.util.DownloadedFiles

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FavoritesScreen(
    container: AppContainer,
    onOpenWallpaper: (Wallpaper) -> Unit,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
    navBarPadding: Dp = 0.dp,
    // Height of the outer top bar (KraftTopBar). Reserved here so the grid
    // starts exactly below the bar. Constant — never shifts.
    topInset: Dp = 0.dp,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
    topBarState: FavoritesTopBarState = FavoritesTopBarState(),
    // Offline repair orchestration. Defaults to the real store; tests inject
    // a fake. When false, no automatic repair runs (Download-all still works).
    offlineRepair: FavoriteOfflineRepair? = null,
    autoRepairOffline: Boolean = true,
) {
    val viewModel: FavoritesViewModel = viewModel(
        factory = viewModelFactory {
            initializer { FavoritesViewModel(container.favoritesRepository) }
        },
    )
    val favorites by viewModel.favorites.collectAsState()
    val collectionsVm: CollectionsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { CollectionsViewModel(container.collectionsRepository) }
        },
    )
    val collections by collectionsVm.collections.collectAsState()
    // Active collection filter; cleared automatically if deleted.
    var activeCollectionId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(collections) {
        if (activeCollectionId != null &&
            collections.none { it.collection.id == activeCollectionId }
        ) {
            activeCollectionId = null
        }
    }
    val activeCollection = collections.firstOrNull { it.collection.id == activeCollectionId }
    val displayedFavorites = remember(favorites, activeCollection) {
        val memberIds = activeCollection?.items?.map { it.wallpaperId }?.toSet()
        if (memberIds == null) favorites
        else favorites.filter { it.wallpaper.id in memberIds }
    }
    val covers = remember(favorites) {
        favorites.associate { fav ->
            fav.wallpaper.id to (
                // Card-sized: original (~300px) first, not large — the strip
                // cards are small and large thumbs waste payload.
                fav.wallpaper.thumbs.original
                    ?: fav.wallpaper.thumbs.small
                    ?: fav.wallpaper.thumbs.large
                    ?: ""
                )
        }
    }
    // The set of downloaded IDs, so the grid can badge cards that are already
    // on disk. Refreshed on resume — a download from the detail screen must
    // show up without restarting the app.
    var downloadedFiles by remember { mutableStateOf(emptyMap<String, DownloadedFile>()) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // Data saver: skip the full-res prefetch on tap (favorites are already
    // local files, so the detail screen loads them instantly anyway).
    var prefetchFullRes by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        prefetchFullRes = !container.settings.current().dataSaverMode
    }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    // Rotation header state — lives here now (was in Settings).
    val rotation by container.rotation.settings.collectAsState(initial = com.wallkraft.app.data.prefs.RotationSettings())
    val rotationCollections by container.collectionsRepository.observeAll()
        .collectAsState(initial = emptyList())
    var rotateRequestId by rememberSaveable { mutableStateOf<String?>(null) }
    var rotateSucceeded by rememberSaveable { mutableStateOf(false) }
    val rotationFailedMsg = stringResource(R.string.rotation_failed)
    LaunchedEffect(rotateRequestId) {
        val idString = rotateRequestId ?: return@LaunchedEffect
        val id = runCatching { java.util.UUID.fromString(idString) }.getOrNull()
        if (id == null) {
            rotateRequestId = null
            return@LaunchedEffect
        }
        var handled = false
        com.wallkraft.app.data.rotation.RotationScheduler.observeRotateNow(context).collect { infos ->
            if (handled) return@collect
            val state = infos.firstOrNull { it.id == id }?.state ?: return@collect
            when (state) {
                androidx.work.WorkInfo.State.SUCCEEDED -> {
                    handled = true
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    rotateSucceeded = true
                    kotlinx.coroutines.delay(1500)
                    rotateSucceeded = false
                    rotateRequestId = null
                }
                androidx.work.WorkInfo.State.FAILED, androidx.work.WorkInfo.State.CANCELLED -> {
                    handled = true
                    snackbarHostState.showSnackbar(rotationFailedMsg)
                    rotateRequestId = null
                }
                else -> Unit
            }
        }
    }
    val repair = offlineRepair
        ?: remember(container) { FavoriteOfflineRepair(container.favoriteImageStore) }
    // Ids with a valid offline copy — drives the "saved offline" header.
    var offlineIds by remember { mutableStateOf(emptySet<String>()) }
    // Batch download progress (done, total); null when idle.
    var repairProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    fun refreshOfflineStatus() {
        scope.launch(Dispatchers.IO) {
            val ids = favorites.mapNotNull { fav ->
                fav.wallpaper.id.takeIf { repair.hasLocal(fav.wallpaper) }
            }.toSet()
            withContext(Dispatchers.Main) { offlineIds = ids }
        }
    }

    // Re-paint offline badges whenever the list changes.
    LaunchedEffect(favorites) { refreshOfflineStatus() }

    fun startDownloadAll() {
        // Acts on the visible list: under a collection filter only the
        // filtered items download (matches the scoped header counts).
        scope.launch {
            val missed = withContext(Dispatchers.IO) {
                repair.missing(displayedFavorites.map { it.wallpaper })
            }
            if (missed.isEmpty()) return@launch
            repairProgress = 0 to missed.size
            val result = withContext(Dispatchers.IO) {
                repair.repairAll(missed) { done, total ->
                    withContext(Dispatchers.Main) { repairProgress = done to total }
                }
            }
            repairProgress = null
            refreshOfflineStatus()
            val res = context.resources
            snackbarHostState.showSnackbar(
                if (result.failed.isEmpty()) {
                    res.getQuantityString(
                        R.plurals.favorites_repair_done, result.restored, result.restored,
                    )
                } else {
                    res.getQuantityString(
                        R.plurals.favorites_repair_failed,
                        result.failed.size, result.failed.size,
                    )
                },
            )
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                downloadedFiles = DownloadedFiles.downloadedFiles(context)
                    .associateBy { it.wallpaperId }
                // Silent repair: restore missing offline copies. Skipped on
                // data saver — bulk downloads are explicit (Download-all).
                if (autoRepairOffline) {
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        if (!container.settings.current().dataSaverMode) {
                            repair.repairAll(repair.missing(favorites.map { it.wallpaper }))
                        }
                        val ids = favorites.mapNotNull { fav ->
                            fav.wallpaper.id.takeIf { repair.hasLocal(fav.wallpaper) }
                        }.toSet()
                        withContext(Dispatchers.Main) { offlineIds = ids }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Selection mode state — derived from selectedIds to prevent desync.
    // Saveable: a wallpaper change relaunches the activity (dynamic-color
    // overlay swap), and plain remember would vaporize selection/filter UI.
    var selectedIds by rememberSaveable { mutableStateOf(emptySet<String>()) }
    val selectionMode = selectedIds.isNotEmpty()
    var pendingRemove by remember { mutableStateOf<List<Wallpaper>?>(null) }
    // Collection dialogs.
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var menuCollectionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var renameCollectionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteCollectionId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Sync shared top bar state (lives outside SharedTransitionLayout).
    // Select-all operates on the visible (possibly collection-filtered) list.
    val visibleIds = displayedFavorites.map { it.wallpaper.id }.toSet()
    val allSelected = visibleIds.isNotEmpty() && selectedIds.containsAll(visibleIds)
    topBarState.selectionMode = selectionMode
    topBarState.selectedCount = selectedIds.size
    topBarState.totalFavorites = favorites.size
    topBarState.allVisibleSelected = allSelected
    topBarState.onCancelSelection = { selectedIds = emptySet() }
    topBarState.onToggleSelectAll = {
        selectedIds = if (allSelected) {
            selectedIds - visibleIds
        } else {
            selectedIds + visibleIds
        }
    }
    topBarState.onDeleteSelected = {
        pendingRemove = favorites
            .filter { it.wallpaper.id in selectedIds }
            .map { it.wallpaper }
    }
    topBarState.onEnterSelectionMode = {
        selectedIds = displayedFavorites.mapTo(mutableSetOf()) { it.wallpaper.id }
    }
    topBarState.onAddToCollection = { showPicker = true }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { Spacer(modifier = Modifier.height(topInset)) },
        snackbarHost = { com.wallkraft.app.core.design.KraftSnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = KraftSpacing.Spacing8, vertical = KraftSpacing.Spacing20),
            verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing16),
        ) {
            // Wallpaper rotation — moved here from Settings. Always visible,
            // even when empty, so you can set it up before adding anything.
            com.wallkraft.app.presentation.settings.SettingsRotationSection(
                settings = rotation,
                collections = rotationCollections,
                rotateWorking = rotateRequestId != null,
                rotateDone = rotateSucceeded,
                onSchedule = { schedule ->
                    scope.launch { container.rotation.setSchedule(schedule) }
                        com.wallkraft.app.data.rotation.RotationScheduler.apply(context, schedule)
                    },
                    onMode = { mode -> scope.launch { container.rotation.setMode(mode) } },
                    onTarget = { target -> scope.launch { container.rotation.setTarget(target) } },
                    onSource = { id -> scope.launch { container.rotation.setSourceCollection(id) } },
                    onRotateNow = {
                        if (rotateRequestId == null) {
                            rotateRequestId = com.wallkraft.app.data.rotation.RotationScheduler.rotateNow(context).toString()
                        }
                    },
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8),
                ) {
                    CollectionStrip(
                        collections = collections,
                        covers = covers,
                        activeId = activeCollectionId,
                        onSelect = { activeCollectionId = it },
                        onNew = { showCreateDialog = true },
                        onMenu = { menuCollectionId = it },
                        modifier = Modifier.padding(
                            top = KraftSpacing.Spacing4,
                            bottom = KraftSpacing.Spacing4,
                        ),
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = KraftSpacing.Spacing8),
                    )
                    // Offline status header: hidden in selection mode and when
                    // everything visible is saved. Shows progress while downloading.
                    // An active filter with nothing displayable gets an empty state
                    // (with a way out) instead of a dead blank grid.
                    val visibleMissing = displayedFavorites.count { it.wallpaper.id !in offlineIds }
                    val progress = repairProgress
                    if (favorites.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.no_favorites_title),
                        message = stringResource(R.string.no_favorites_message),
                        icon = Icons.Outlined.FavoriteBorder,
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = KraftSpacing.GlassBarReserve),
                    )
                } else if (displayedFavorites.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.no_results_title),
                        message = stringResource(R.string.no_results_hint_filters),
                        icon = Icons.Outlined.FilterAlt,
                        actionLabel = stringResource(R.string.clear_filter),
                        onAction = { activeCollectionId = null },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    // Apple way: no persistent "X of Y saved • Download all" chrome.
                    // Offline is silent (ON_RESUME repair) + on-demand (zoom/set).
                    // Progress still shows if a bulk repair is somehow in flight.
                    if (progress != null) {
                        val (done, total) = progress
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = KraftSpacing.Spacing16,
                                    vertical = KraftSpacing.Spacing8,
                                ),
                        ) {
                            Text(
                                text = "$done / $total",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(end = KraftSpacing.Spacing12),
                            )
                            LinearProgressIndicator(
                                progress = { done.toFloat() / total.coerceAtLeast(1) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    WallpaperGrid(
                        wallpapers = displayedFavorites.map { it.wallpaper },
                onOpen = { wallpaper ->
                    if (selectionMode) {
                        selectedIds = if (wallpaper.id in selectedIds) {
                            selectedIds - wallpaper.id
                        } else {
                            selectedIds + wallpaper.id
                        }
                    } else {
                        onOpenWallpaper(wallpaper)
                    }
                },
                onLoadMore = {},
                state = gridState,
                downloadedIds = downloadedFiles.keys,
                prefetchFullRes = prefetchFullRes,
                onLongClick = { wallpaper ->
                    selectedIds = setOf(wallpaper.id)
                },
                selectionMode = selectionMode,
                selectedIds = selectedIds,
                onToggleSelect = { wallpaper ->
                    selectedIds = if (wallpaper.id in selectedIds) {
                        selectedIds - wallpaper.id
                    } else {
                        selectedIds + wallpaper.id
                    }
                },
                modifier = Modifier.weight(1f),
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
                }
            }
        }
    }

    // Remove confirmation dialog
    pendingRemove?.let { wallpapersToRemove ->
        val count = wallpapersToRemove.size
        AlertDialog(
            onDismissRequest = { pendingRemove = null },
            title = {
                Text(
                    if (count == 1) {
                        stringResource(R.string.remove_favorites_confirm_title)
                    } else {
                        pluralStringResource(
                            R.plurals.remove_favorites_selected_title,
                            count,
                            count,
                        )
                    },
                )
            },
            text = {
                Text(
                    if (count == 1) {
                        stringResource(R.string.remove_favorites_confirm_message)
                    } else {
                        pluralStringResource(
                            R.plurals.remove_favorites_selected_message,
                            count,
                            count,
                        )
                    },
                )
            },
            confirmButton = {
                val haptic = LocalHapticFeedback.current
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        val removedIds = wallpapersToRemove.mapTo(mutableSetOf()) { it.id }
                        // Unfavoriting cascades: membership rows vanish with the
                        // favorite. Count affected collections BEFORE removal so
                        // the user is told — never a silent strip.
                        val stripped = collections.count { entry ->
                            entry.items.any { it.wallpaperId in removedIds }
                        }
                        wallpapersToRemove.forEach { viewModel.remove(it.id) }
                        selectedIds = selectedIds - removedIds
                        pendingRemove = null
                        if (stripped > 0) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.resources.getQuantityString(
                                        R.plurals.favorites_removed_from_collections,
                                        stripped,
                                        stripped,
                                    ),
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemove = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Add-to-collection picker (selection mode only — clearing the selection
    // closes it).
    if (showPicker && selectionMode) {
        AddToCollectionDialog(
            collections = collections,
            selectedIds = selectedIds,
            onToggle = { collectionId, member ->
                selectedIds.forEach { wallpaperId ->
                    collectionsVm.setMember(collectionId, wallpaperId, member)
                }
            },
            // Creating from the picker checks the new collection for the
            // current selection right away — no create-then-hunt.
            onCreate = { name ->
                val targets = selectedIds.toList()
                collectionsVm.create(name) { id ->
                    if (id > 0) targets.forEach { collectionsVm.setMember(id, it, true) }
                }
            },
            onDismiss = {
                showPicker = false
                // Done — exit selection so the grid returns to normal.
                // Previously this only hid the dialog, leaving the
                // selection active and the top bar stuck in select mode.
                selectedIds = emptySet()
            },
        )
    }

    // New collection, from the strip.
    if (showCreateDialog) {
        RenameCollectionDialog(
            title = stringResource(R.string.new_collection),
            current = "",
            onDismiss = { showCreateDialog = false },
            onSave = { name ->
                collectionsVm.create(name)
                showCreateDialog = false
            },
            confirmLabel = R.string.create,
        )
    }

    // Long-press menu on a collection card.
    val menuEntry = menuCollectionId?.let { id ->
        collections.firstOrNull { it.collection.id == id }
    }
    if (menuEntry != null) {
        CollectionMenuDialog(
            name = menuEntry.collection.name,
            onRename = {
                renameCollectionId = menuEntry.collection.id
                menuCollectionId = null
            },
            onDelete = {
                deleteCollectionId = menuEntry.collection.id
                menuCollectionId = null
            },
            onDismiss = { menuCollectionId = null },
        )
    }

    // Rename.
    val renameEntry = renameCollectionId?.let { id ->
        collections.firstOrNull { it.collection.id == id }
    }
    if (renameEntry != null) {
        RenameCollectionDialog(
            title = stringResource(R.string.rename),
            current = renameEntry.collection.name,
            onDismiss = { renameCollectionId = null },
            onSave = { name ->
                // Duplicate names stay open with a notice instead of
                // closing silently (or crashing, as before).
                collectionsVm.rename(renameEntry.collection.id, name) { ok ->
                    if (ok) {
                        renameCollectionId = null
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.collection_name_exists),
                            )
                        }
                    }
                }
            },
        )
    }

    // Delete (members cascade; the wallpapers stay in Favorites).
    val deleteEntry = deleteCollectionId?.let { id ->
        collections.firstOrNull { it.collection.id == id }
    }
    if (deleteEntry != null) {
        DeleteCollectionDialog(
            name = deleteEntry.collection.name,
            onDismiss = { deleteCollectionId = null },
            onConfirm = {
                // Snapshot members first: delete cascades the rows away.
                val restoreName = deleteEntry.collection.name
                val restoreMembers = deleteEntry.items.map { it.wallpaperId }
                collectionsVm.delete(deleteEntry.collection.id)
                // Leaving an active filter on a deleted collection shows a
                // dead empty grid — drop back to All immediately.
                if (activeCollectionId == deleteEntry.collection.id) activeCollectionId = null
                deleteCollectionId = null
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.collection_deleted),
                        actionLabel = context.getString(R.string.undo),
                        duration = androidx.compose.material3.SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        collectionsVm.restore(restoreName, restoreMembers)
                    }
                }
            },
        )
    }
}
