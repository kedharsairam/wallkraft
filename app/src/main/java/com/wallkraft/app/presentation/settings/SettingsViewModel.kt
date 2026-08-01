package com.wallkraft.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallkraft.app.domain.model.AppSettings
import com.wallkraft.app.domain.model.Order
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.domain.model.ThemeMode
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

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
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
        appScope.launch {
            _apiKeyText
                .debounce(500)
                .collect { key ->
                    if (seedComplete && key != lastPersisted) {
                        settingsRepository.update { it.copy(apiKey = key) }
                        lastPersisted = key
                    }
                }
        }
    }

    fun setApiKey(key: String) {
        hasUserInput = true
        _apiKeyText.value = key
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.update { it.copy(themeMode = mode) } }
    }

    fun setSorting(sorting: Sorting) {
        viewModelScope.launch { settingsRepository.update { it.copy(sorting = sorting) } }
    }

    fun setOrder(order: Order) {
        viewModelScope.launch { settingsRepository.update { it.copy(order = order) } }
    }

    override fun onCleared() {
        // If the user typed and navigated away inside the 500ms debounce
        // window, the pending write was cancelled. Flush the latest value on
        // the app scope so no keystrokes are silently lost — without blocking
        // the main thread.
        val latest = _apiKeyText.value
        if (hasUserInput && latest != lastPersisted) {
            appScope.launch { settingsRepository.update { it.copy(apiKey = latest) } }
        }
    }
}
