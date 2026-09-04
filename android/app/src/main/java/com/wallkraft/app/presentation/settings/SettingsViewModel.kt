package com.wallkraft.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallkraft.app.data.api.WallhavenApi
import com.wallkraft.app.domain.model.AppSettings
import com.wallkraft.app.domain.model.Category
import com.wallkraft.app.domain.model.Orientation
import com.wallkraft.app.domain.model.Purity
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview

@OptIn(FlowPreview::class)
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val api: WallhavenApi,
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
                    android.util.Log.d("SettingsViewModel", "Debounced key (length=${trimmed.length})")
                    if (seedComplete && trimmed != lastPersisted) {
                        // Show verifying state while the API call is in flight.
                        _isValidating.value = true
                        try {
                            // Validate the key against the API
                            val isValid = api.validateApiKey(trimmed)
                            android.util.Log.d("SettingsViewModel", "API key validation result: $isValid")
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
                    // Cancel the scope after the flush completes so it doesn't leak.
                    appScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
                }
            }
        } else {
            appScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        }
    }
}
