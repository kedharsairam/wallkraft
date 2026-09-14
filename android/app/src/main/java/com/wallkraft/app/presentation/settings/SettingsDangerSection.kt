package com.wallkraft.app.presentation.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.wallkraft.app.R
import com.wallkraft.app.core.design.KraftSpacing
import com.wallkraft.app.core.utils.KraftHaptics
import com.wallkraft.app.domain.usecase.WipeAllDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.system.exitProcess

/**
 * Runs the GDPR "delete all data" wipe, then restarts the process so every
 * in-memory singleton (ViewModels, Hilt graph, loaders) re-seeds defaults.
 */
@HiltViewModel
class DangerZoneViewModel @Inject constructor(
    private val wipeAllData: WipeAllDataUseCase,
) : ViewModel() {

    private val _isWiping = MutableStateFlow(false)
    val isWiping: StateFlow<Boolean> = _isWiping.asStateFlow()

    suspend fun wipeAll() {
        if (_isWiping.value) return
        _isWiping.value = true
        try {
            withContext(Dispatchers.IO) { wipeAllData.wipeAll() }
        } finally {
            _isWiping.value = false
        }
    }
}

/**
 * Danger zone — destructive section at the bottom of Settings.
 *
 * Red "Delete all data" button → confirmation dialog with the consequence
 * list → wipe on confirm → process restart into a fresh state. No snackbar:
 * the restart itself is the confirmation.
 */
@Composable
fun SettingsDangerSection() {
    val viewModel: DangerZoneViewModel = hiltViewModel()
    val isWiping by viewModel.isWiping.collectAsState()
    var showConfirm by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    SettingsGroup(title = stringResource(R.string.danger_zone_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8)) {
            Text(
                text = stringResource(R.string.delete_all_data),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = stringResource(R.string.delete_all_data_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = {
                    KraftHaptics.buttonPress(haptic)
                    showConfirm = true
                },
                enabled = !isWiping,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.delete_all_data))
            }
        }
    }

    if (showConfirm) {
        DeleteAllDataDialog(
            isWiping = isWiping,
            onDismiss = { if (!isWiping) showConfirm = false },
            onConfirm = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    viewModel.wipeAll()
                    restartApp(context)
                }
            },
        )
    }
}

@Composable
private fun DeleteAllDataDialog(
    isWiping: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_all_data_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(KraftSpacing.Spacing8)) {
                Text(
                    text = stringResource(R.string.delete_all_data_dialog_message),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (isWiping) {
                    Text(
                        text = stringResource(R.string.delete_all_data_deleting),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isWiping) {
                if (isWiping) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(KraftSpacing.Spacing20),
                        strokeWidth = KraftSpacing.Spacing2,
                    )
                    Spacer(Modifier.width(KraftSpacing.Spacing8))
                }
                Text(
                    text = stringResource(R.string.delete_all_data),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isWiping) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * Relaunches the app from the launcher intent and kills this process, so no
 * stale ViewModel/Hilt/loader state survives the wipe. Defaults re-seed from
 * the now-empty stores on the next cold start.
 */
private fun restartApp(context: Context) {
    runCatching {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(launch)
        }
    }
    exitProcess(0)
}
