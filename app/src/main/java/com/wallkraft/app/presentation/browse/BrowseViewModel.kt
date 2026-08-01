package com.wallkraft.app.presentation.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.repository.SettingsRepository
import com.wallkraft.app.domain.repository.WallpaperError
import com.wallkraft.app.domain.repository.WallpaperRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BrowseUiState(
    val wallpapers: List<Wallpaper> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isAppending: Boolean = false,
    val error: String? = null,
    val filters: WallhavenFilters = WallhavenFilters(),
    val query: String = "",
    val currentPage: Int = 1,
    val lastPage: Int = 1,
    val hasMore: Boolean = true,
    val rateLimited: Boolean = false,
)

class BrowseViewModel(
    private val repository: WallpaperRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            _uiState.update {
                it.copy(
                    filters = WallhavenFilters(
                        categories = settings.categories,
                        purity = settings.purity,
                        sorting = settings.sorting,
                        order = settings.order,
                    ),
                )
            }
            loadFirstPage()
        }
        viewModelScope.launch {
            repository.observeRateLimited().collect { limited ->
                _uiState.update { it.copy(rateLimited = limited) }
            }
        }
    }

    fun search(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        loadFirstPage()
    }

    fun setFilters(filters: WallhavenFilters) {
        _uiState.update { it.copy(filters = filters.copy(query = it.query)) }
        loadFirstPage()
    }

    fun retry() = loadFirstPage()

    fun loadFirstPage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isInitialLoading = true, error = null) }
            val filters = _uiState.value.filters
            repository.search(filters, 1)
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            wallpapers = response.data,
                            isInitialLoading = false,
                            isAppending = false,
                            currentPage = response.meta.currentPage,
                            lastPage = response.meta.lastPage,
                            hasMore = response.meta.currentPage < response.meta.lastPage,
                            error = null,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isInitialLoading = false, error = e.toUserMessage())
                    }
                }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isInitialLoading || state.isAppending || !state.hasMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAppending = true) }
            repository.search(state.filters, state.currentPage + 1)
                .onSuccess { response ->
                    _uiState.update { cur ->
                        cur.copy(
                            wallpapers = cur.wallpapers + response.data,
                            isAppending = false,
                            currentPage = response.meta.currentPage,
                            lastPage = response.meta.lastPage,
                            hasMore = response.meta.currentPage < response.meta.lastPage,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isAppending = false) }
                }
        }
    }
}

fun Throwable.toUserMessage(): String = when (this) {
    is WallpaperError.RateLimited -> "Rate limit reached. Please wait a moment and try again."
    is WallpaperError.Api -> message ?: "Something went wrong. Check your connection."
    else -> message ?: "Something went wrong."
}
