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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.core.utils.KraftHaptics
import com.wallkraft.app.data.prefs.RotationSettings
import com.wallkraft.app.data.rotation.RotationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

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
    navBarPadding: Dp = 0.dp,
    topInset: Dp = 0.dp,
    onHistoryClick: () -> Unit = {},
) {
    SettingsScreenImpl(navBarPadding = navBarPadding, topInset = topInset, onHistoryClick = onHistoryClick)
}

@Composable
private fun SettingsScreenImpl(
    navBarPadding: Dp = 0.dp,
    topInset: Dp = 0.dp,
    onHistoryClick: () -> Unit = {},
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsState()
    val apiKeyText by viewModel.apiKeyText.collectAsState()
    val isValidating by viewModel.isValidating.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showApiDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var cacheSizeText by remember { mutableStateOf("—") }
    val githubUrl = stringResource(R.string.github_url)

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
    val crashLogsDeletedMsg = stringResource(R.string.crash_logs_deleted)
    val upToDateMsg = stringResource(R.string.update_up_to_date)
    val checkFailedMsg = stringResource(R.string.update_check_failed)
    var showUpdateDialog by remember { mutableStateOf<com.wallkraft.app.domain.model.AppUpdateInfo?>(null) }
    val downloadState by viewModel.downloadState.collectAsState()
    LaunchedEffect(updateState) {
        when (val s = updateState) {
            is UpdateUiState.Available -> showUpdateDialog = s.info
            is UpdateUiState.UpToDate -> {
                // Only snackbar when user tapped Check (state leaves Idle). Silent otherwise.
                snackbarHostState.showSnackbar(upToDateMsg)
                viewModel.clearUpdateState()
            }
            is UpdateUiState.Error -> {
                snackbarHostState.showSnackbar(s.message.ifBlank { checkFailedMsg })
                viewModel.clearUpdateState()
            }
            else -> Unit
        }
    }
    // When download finishes, launch system installer — this IS the update.
    LaunchedEffect(downloadState) {
        val d = downloadState
        if (d is SettingsViewModel.DownloadUiState.Downloaded) {
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    d.file,
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                snackbarHostState.showSnackbar(checkFailedMsg)
            } finally {
                showUpdateDialog = null
                viewModel.clearUpdateState()
                viewModel.clearDownloadState()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { com.wallkraft.app.core.design.KraftSnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KraftSpacing.Spacing8)
                .padding(top = topInset + KraftSpacing.Spacing20, bottom = KraftSpacing.Spacing20),
            verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing20),
        ) {
            SettingsBrowsingSection(
                settings = settings,
                onCategories = viewModel::setCategories,
                onPurity = viewModel::setPurity,
                onSorting = viewModel::setSorting,
                onTopRange = viewModel::setTopRange,
                onOrientation = viewModel::setOrientation,
            )

            SettingsDataSection(
                dataSaverMode = settings.dataSaverMode,
                onDataSaverChange = viewModel::setDataSaverMode,
                cacheSizeText = cacheSizeText,
                onClearCacheClick = { showClearCacheDialog = true },
            )

            SettingsHistorySection(onClick = onHistoryClick)

            SettingsAdvancedSection(
                apiKeyText = apiKeyText,
                isValidating = isValidating,
                apiKeyValid = settings.apiKeyValid,
                onApiClick = { showApiDialog = true },
            )

            SettingsSupportSection()

            SettingsAboutSection(
                githubUrl = githubUrl,
                onPrivacyClick = { showPrivacyDialog = true },
                updateState = updateState,
                onCheckUpdates = { viewModel.checkForUpdates() },
                onShareCrashLogClick = {
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
                onDeleteCrashLogClick = {
                    val deleted = CrashLogs.deleteAll(context)
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (deleted > 0) crashLogsDeletedMsg else noCrashLogsMsg,
                        )
                    }
                },
            )

            SettingsDangerSection()

            Spacer(Modifier.height(KraftSpacing.GlassBarReserve))
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

    showUpdateDialog?.let { info ->
        UpdateAvailableDialog(
            info = info,
            downloadState = downloadState,
            onDismiss = {
                showUpdateDialog = null
                viewModel.clearUpdateState()
                viewModel.clearDownloadState()
            },
            onDownload = {
                KraftHaptics.buttonPress(haptic)
                val dir = File(context.cacheDir, "update")
                viewModel.downloadUpdate(info, dir)
            },
        )
    }
}
