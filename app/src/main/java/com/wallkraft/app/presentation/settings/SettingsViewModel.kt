package com.wallkraft.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallkraft.app.domain.model.AppSettings
import com.wallkraft.app.domain.model.Order
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.domain.model.ThemeMode
import com.wallkraft.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    fun setApiKey(key: String) {
        viewModelScope.launch { settingsRepository.update { it.copy(apiKey = key) } }
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
}
