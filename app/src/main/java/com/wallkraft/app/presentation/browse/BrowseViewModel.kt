package com.wallkraft.app.presentation.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.repository.SettingsRepository
import com.wallkraft.app.domain.repository.WallpaperRepository
import com.wallkraft.app.util.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BrowseUiState(
    val wallpapers: List<Wallpaper> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isAppending: Boolean = false,
    val isRefreshing: Boolean = false,
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
    private val errorMessage: (Throwable) -> String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    /**
     * The single in-flight request. Starting a new first page cancels the
     * previous one so a slow response for an older query/filter can never
     * overwrite newer results (last-write-wins race).
     */
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            _uiState.update {
                it.copy(
                    filters = WallhavenFilters(
                        categories = settings.categories,
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
        _uiState.update {
            it.copy(
                query = newQuery,
                filters = it.filters.copy(query = newQuery),
                // A new query replaces the whole list: drop the old results so
                // a failure shows the error state instead of silently keeping
                // stale wallpapers from the previous search.
                wallpapers = emptyList(),
            )
        }
        loadFirstPage()
    }

    fun setFilters(filters: WallhavenFilters) {
        _uiState.update {
            it.copy(
                filters = filters.copy(query = it.query),
                wallpapers = emptyList(),
            )
        }
        loadFirstPage()
    }

    fun retry() = loadFirstPage()

    /** Pull-to-refresh: re-fetch page 1 without clearing the list. */
    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            val filters = _uiState.value.filters
            try {
                repository.search(filters, 1)
                    .onSuccess { response ->
                        _uiState.update {
                            it.copy(
                                wallpapers = response.data,
                                isRefreshing = false,
                                currentPage = response.meta.currentPage,
                                lastPage = response.meta.lastPage,
                                hasMore = response.meta.currentPage < response.meta.lastPage,
                                error = null,
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(isRefreshing = false, error = errorMessage(e))
                        }
                    }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun loadFirstPage() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isInitialLoading = true, error = null) }
            val filters = _uiState.value.filters
            try {
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
                            it.copy(isInitialLoading = false, error = errorMessage(e))
                        }
                    }
            } finally {
                // Also runs when the job is cancelled by a newer search, so the
                // UI never gets stuck on a spinner.
                _uiState.update { it.copy(isInitialLoading = false, isAppending = false) }
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isInitialLoading || state.isAppending || !state.hasMore) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isAppending = true) }
            try {
                repository.search(state.filters, state.currentPage + 1)
                    .onSuccess { response ->
                        _uiState.update { cur ->
                            cur.copy(
                                // The Wallhaven API can return the same wallpaper on
                                // different pages; dedupe at the accumulation point so
                                // the grid's id keys never collide.
                                wallpapers = (cur.wallpapers + response.data).distinctBy { it.id },
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
            } finally {
                _uiState.update { it.copy(isAppending = false) }
            }
        }
    }
}
