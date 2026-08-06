package com.wallkraft.app.presentation.tag

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

data class TagUiState(
    val tag: String = "",
    val wallpapers: List<Wallpaper> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isAppending: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val lastPage: Int = 1,
    val hasMore: Boolean = true,
    val rateLimited: Boolean = false,
    internal val filters: WallhavenFilters = WallhavenFilters(),
)

/**
 * Fetches a tag-filtered wallpaper list — a slimmed-down [com.wallkraft.app.presentation.browse.BrowseViewModel]
 * that fixes the query to a single tag and drops search/filter UI state.
 *
 * The tag is quoted in the query so Wallhaven matches the exact tag phrase
 * (e.g. "blue eyes") instead of AND-ing the words.
 */
class TagViewModel(
    private val tag: String,
    private val repository: WallpaperRepository,
    settingsRepository: SettingsRepository,
    private val errorMessage: (Throwable) -> String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagUiState(tag = tag))
    val uiState: StateFlow<TagUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            _uiState.update {
                it.copy(
                    filters = WallhavenFilters(
                        categories = settings.categories,
                        sorting = settings.sorting,
                        orientation = settings.orientation,
                        query = "\"$tag\"",
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

    private fun loadFirstPage() {
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
                                wallpapers = (cur.wallpapers + response.data).distinctBy { it.id },
                                isAppending = false,
                                currentPage = response.meta.currentPage,
                                lastPage = response.meta.lastPage,
                                hasMore = response.meta.currentPage < response.meta.lastPage,
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(isAppending = false, error = errorMessage(e))
                        }
                    }
            } finally {
                _uiState.update { it.copy(isAppending = false) }
            }
        }
    }

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

    fun retry() = loadFirstPage()
}
