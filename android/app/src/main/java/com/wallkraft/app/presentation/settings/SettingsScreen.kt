package com.wallkraft.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import androidx.core.content.FileProvider
import com.wallkraft.app.util.CrashLogs
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.wallkraft.app.AppContainer
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.data.prefs.RotationSettings
import com.wallkraft.app.data.rotation.RotationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Settings screen assembly.
 *
 * Previously a 780-line god object; now each section lives in its own file
 * (Browsing, Data, Support, Advanced, About) plus shared components and
 * dialogs. This file owns ViewModel wiring, dialog state, and layout only —
 * no section UI.
 */
@Composable
fun SettingsScreen(
    container: AppContainer,
    navBarPadding: Dp = 0.dp,
    // Height of the outer top bar (KraftTopBar). Reserved here so the content
    // starts exactly below the bar. Constant — never shifts.
    topInset: Dp = 0.dp,
) {
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory { initializer { SettingsViewModel(container.settings, container.api) } },
    )
    val settings by viewModel.settings.collectAsState()
    val apiKeyText by viewModel.apiKeyText.collectAsState()
    val isValidating by viewModel.isValidating.collectAsState()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showApiDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var cacheSizeText by remember { mutableStateOf("—") }
    val githubUrl = stringResource(R.string.github_url)

    // Compute cache size — refresh on every ON_START so returning from
    // detail screen (where a download may have happened) shows current size.
    fun refreshCacheSize() {
        scope.launch(Dispatchers.IO) {
            val cacheDir = context.cacheDir
            val searchCache = File(cacheDir, "search_cache")
            val coilCache = File(cacheDir, "coil")
            var total = 0L
            listOf(searchCache, coilCache, File(cacheDir, "image_cache")).forEach { dir ->
                if (dir.exists()) total += dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            }
            cacheSizeText = formatBytes(total)
        }
    }
    LaunchedEffect(Unit) { refreshCacheSize() }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                refreshCacheSize()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val cacheClearedMsg = stringResource(R.string.cache_cleared)
    val apiSavedMsg = stringResource(R.string.api_key_saved)
    val noCrashLogsMsg = stringResource(R.string.no_crash_logs)
    val rotationStartedMsg = stringResource(R.string.rotation_started)

    // Rotation settings + collections for the source picker.
    val rotation by container.rotation.settings.collectAsState(initial = RotationSettings())
    val rotationCollections by container.collectionsRepository.observeAll()
        .collectAsState(initial = emptyList())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { Spacer(modifier = Modifier.height(topInset)) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KraftSpacing.Spacing20, vertical = KraftSpacing.Spacing16)
                .padding(bottom = navBarPadding),
            verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing24),
        ) {
            SettingsBrowsingSection(
                settings = settings,
                onCategories = viewModel::setCategories,
                onPurity = viewModel::setPurity,
                onSorting = viewModel::setSorting,
                onOrientation = viewModel::setOrientation,
            )

            SettingsDataSection(
                dataSaverMode = settings.dataSaverMode,
                onDataSaverChange = viewModel::setDataSaverMode,
                cacheSizeText = cacheSizeText,
                onClearCacheClick = { showClearCacheDialog = true },
            )

            SettingsRotationSection(
                settings = rotation,
                collections = rotationCollections,
                onSchedule = { schedule ->
                    scope.launch {
                        container.rotation.setSchedule(schedule)
                    }
                    RotationScheduler.apply(context, schedule)
                },
                onMode = { mode ->
                    scope.launch { container.rotation.setMode(mode) }
                },
                onTarget = { target ->
                    scope.launch { container.rotation.setTarget(target) }
                },
                onSource = { id ->
                    scope.launch { container.rotation.setSourceCollection(id) }
                },
                onRotateNow = {
                    RotationScheduler.rotateNow(context)
                    scope.launch { snackbarHostState.showSnackbar(rotationStartedMsg) }
                },
            )

            SettingsSupportSection()

            SettingsAdvancedSection(
                apiKeyText = apiKeyText,
                isValidating = isValidating,
                apiKeyValid = settings.apiKeyValid,
                onApiClick = { showApiDialog = true },
            )

            SettingsAboutSection(
                githubUrl = githubUrl,
                onPrivacyClick = { showPrivacyDialog = true },
                onShareCrashLogClick = {
                    // Local-only: the newest crash log is handed to the system
                    // share sheet. Nothing is transmitted by the app itself —
                    // the user chooses where it goes, per incident.
                    val log = CrashLogs.latestCrashLog(context)
                    if (log == null) {
                        scope.launch { snackbarHostState.showSnackbar(noCrashLogsMsg) }
                    } else {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            log,
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    }
                },
            )
        }
    }

    if (showApiDialog) {
        ApiKeyDialog(
            initial = apiKeyText,
            onDismiss = { showApiDialog = false },
            onSave = { key ->
                viewModel.setApiKey(key)
                showApiDialog = false
                scope.launch { snackbarHostState.showSnackbar(apiSavedMsg) }
            },
        )
    }

    if (showClearCacheDialog) {
        ClearCacheDialog(
            onDismiss = { showClearCacheDialog = false },
            onConfirm = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showClearCacheDialog = false
                scope.launch(Dispatchers.IO) {
                    File(context.cacheDir, "search_cache").deleteRecursively()
                    File(context.cacheDir, "coil").deleteRecursively()
                    File(context.cacheDir, "image_cache").deleteRecursively()
                    // Update UI on main thread first, then show snackbar.
                    withContext(Dispatchers.Main) {
                        cacheSizeText = formatBytes(0)
                        snackbarHostState.showSnackbar(cacheClearedMsg)
                    }
                }
            },
        )
    }

    if (showPrivacyDialog) {
        PrivacyDialog(onDismiss = { showPrivacyDialog = false })
    }
}
