package com.wallkraft.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallkraft.app.data.api.GithubApi
import com.wallkraft.app.data.api.WallhavenApi
import com.wallkraft.app.core.errors.AppError
import com.wallkraft.app.core.utils.Result
import com.wallkraft.app.domain.model.AppSettings
import com.wallkraft.app.domain.model.AppUpdateInfo
import com.wallkraft.app.domain.model.Category
import com.wallkraft.app.domain.model.Orientation
import com.wallkraft.app.domain.model.Purity
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.domain.model.TopRange
import com.wallkraft.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class Available(val info: AppUpdateInfo) : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val api: WallhavenApi,
    private val githubApi: GithubApi,
    private val errorMessage: @JvmSuppressWildcards (AppError) -> String,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings(),
        )

    /**
     * The live text-field value. The UI binds to this so keystrokes appear
     * instantly; persistence below is debounced so a pasted/typed key doesn't
     * rewrite the DataStore file on every character.
     */
    private val _apiKeyText = MutableStateFlow("")
    val apiKeyText: StateFlow<String> = _apiKeyText.asStateFlow()

    /** True while an API key validation request is in flight. */
    private val _isValidating = MutableStateFlow(false)
    val isValidating: StateFlow<Boolean> = _isValidating.asStateFlow()

    private var hasUserInput = false

    /** Last value written to DataStore; null until the seed read completes. */
    private var lastPersisted: String? = null
    private var seedComplete = false

    /**
     * Outlives [viewModelScope], which is already cancelled when [onCleared]
     * runs. The debounced persist and the final flush both live here so the
     * main thread is never blocked on disk I/O (no `runBlocking`).
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Seed the field from the persisted key once DataStore answers. Only
        // if the user hasn't already started typing (guards a slow load racing
        // the first keystroke).
        viewModelScope.launch {
            val persisted = settingsRepository.settings.first().apiKey
            lastPersisted = persisted
            seedComplete = true
            if (!hasUserInput) _apiKeyText.value = persisted
        }

        // Debounced persist: a burst of keystrokes becomes one write 500ms
        // after the last one. Skips writes before the seed completes (so a
        // slow first read can never wipe the stored key) and skips writes that
        // don't change the persisted value (so seeding is a no-op).
        // When the API key is removed, NSFW is automatically stripped from purity.
        // When the API key changes, it is validated against the Wallhaven API.
        appScope.launch {
            _apiKeyText
                .debounce(500)
                .collect { key ->
                    val trimmed = key.trim()
                    if (com.wallkraft.app.BuildConfig.DEBUG) android.util.Log.d("SettingsViewModel", "Debounced key (length=${trimmed.length})")
                    if (seedComplete && trimmed != lastPersisted) {
                        // Show verifying state while the API call is in flight.
                        _isValidating.value = true
                        try {
                            // Validate the key against the API
                            val isValid = api.validateApiKey(trimmed)
                            if (com.wallkraft.app.BuildConfig.DEBUG) android.util.Log.d("SettingsViewModel", "API key validation result: $isValid")
                            settingsRepository.update { current ->
                                val updated = current.copy(apiKey = trimmed, apiKeyValid = isValid)
                                if (trimmed.isBlank() || !isValid) {
                                    // No API key or invalid — strip NSFW from purity.
                                    updated.copy(purity = updated.purity - Purity.NSFW)
                                } else {
                                    updated
                                }
                            }
                            lastPersisted = trimmed
                        } finally {
                            _isValidating.value = false
                        }
                    }
                }
        }
    }

    fun setApiKey(key: String) {
        hasUserInput = true
        _apiKeyText.value = key
        // Show verifying state immediately — don't wait for the 500ms debounce.
        _isValidating.value = key.isNotBlank()
    }

    fun setSorting(sorting: Sorting) {
        viewModelScope.launch { settingsRepository.update { it.copy(sorting = sorting) } }
    }

    fun setTopRange(topRange: TopRange) {
        viewModelScope.launch { settingsRepository.update { it.copy(topRange = topRange) } }
    }

    fun setOrientation(orientation: Orientation) {
        viewModelScope.launch { settingsRepository.update { it.copy(orientation = orientation) } }
    }

    fun setCategories(categories: Set<Category>) {
        // Never allow empty — at least one category must be selected.
        if (categories.isEmpty()) return
        viewModelScope.launch { settingsRepository.update { it.copy(categories = categories) } }
    }

    fun setPurity(purity: Set<Purity>) {
        if (purity.isEmpty()) return
        viewModelScope.launch { settingsRepository.update { it.copy(purity = purity) } }
    }

    fun setDataSaverMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.update { it.copy(dataSaverMode = enabled) } }
    }

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()
    private var updateJob: kotlinx.coroutines.Job? = null

    /** Manual update check only — no auto, no worker. Caller is Settings About row. */
    fun checkForUpdates() {
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            _updateState.value = UpdateUiState.Checking
            when (val result = githubApi.latestRelease()) {
                is Result.Success -> {
                    val info = result.data
                    _updateState.value = if (info == null) UpdateUiState.UpToDate else UpdateUiState.Available(info)
                }
                is Result.Failure -> {
                    // RateLimited/offline → silent UpToDate to avoid nagging, else mapped message
                    _updateState.value = when (result.error) {
                        is AppError.NetworkError.RateLimited,
                        is AppError.NetworkError.NoConnection,
                        is AppError.NetworkError.Timeout -> UpdateUiState.UpToDate
                        else -> UpdateUiState.Error(errorMessage(result.error))
                    }
                }
            }
        }
    }

    fun clearUpdateState() {
        _updateState.value = UpdateUiState.Idle
    }

    sealed interface DownloadUiState {
        data object Idle : DownloadUiState
        data class Downloading(val read: Long, val total: Long) : DownloadUiState
        data class Downloaded(val file: java.io.File) : DownloadUiState
        data class Error(val message: String) : DownloadUiState
    }

    private val _downloadState = MutableStateFlow<DownloadUiState>(DownloadUiState.Idle)
    val downloadState: StateFlow<DownloadUiState> = _downloadState.asStateFlow()
    private var downloadJob: kotlinx.coroutines.Job? = null

    fun downloadUpdate(info: com.wallkraft.app.domain.model.AppUpdateInfo, destDir: java.io.File) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _downloadState.value = DownloadUiState.Downloading(0, info.sizeBytes)
            val dest = java.io.File(destDir, "wallkraft-${info.version}.apk")
            // Clean old apks first (bounded cache)
            destDir.listFiles()?.forEach { if (it.name != dest.name) it.delete() }
            when (val r = githubApi.downloadTo(info.apkUrl, dest, { read, total ->
                _downloadState.value = DownloadUiState.Downloading(read, total)
            })) {
                is Result.Success -> _downloadState.value = DownloadUiState.Downloaded(dest)
                is Result.Failure -> _downloadState.value = DownloadUiState.Error(errorMessage(r.error))
            }
        }
    }

    fun clearDownloadState() {
        downloadJob?.cancel()
        _downloadState.value = DownloadUiState.Idle
    }

    override fun onCleared() {
        // If the user typed and navigated away inside the 500ms debounce
        // window, the pending write was cancelled. Flush the latest value on
        // the app scope so no keystrokes are silently lost — without blocking
        // the main thread.
        val latest = _apiKeyText.value
        if (hasUserInput && latest != lastPersisted) {
            appScope.launch {
                _isValidating.value = true
                try {
                    val isValid = api.validateApiKey(latest)
                    settingsRepository.update { current ->
                        val updated = current.copy(apiKey = latest, apiKeyValid = isValid)
                        if (latest.isBlank() || !isValid) {
                            updated.copy(purity = updated.purity - Purity.NSFW)
                        } else {
                            updated
                        }
                    }
                } finally {
                    _isValidating.value = false
                }
            }
        }
        // Cancel appScope after launching the flush — the flush coroutine
        // is now running and will complete independently. Cancelling here
        // (instead of in the flush's finally) avoids a race where the debounce
        // collection on the same scope gets killed mid-flight.
        appScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }
}
