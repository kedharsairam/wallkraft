package com.wallkraft.app.presentation.common

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.repository.SettingsRepository
import com.wallkraft.app.domain.repository.WallpaperRepository
import com.wallkraft.app.util.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Shared state for any screen that shows a paginated wallpaper grid fed by
 * [WallpaperRepository.search] — the Browse tab and the tag-as-browse screen
 * both use this. The only difference between them is the initial query.
 */
data class WallpaperListUiState(
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

/**
 * The pagination + refresh + filter machinery shared by every wallpaper list
 * screen. [initialQuery] seeds the search box and the API query — the Browse
 * tab passes "" (empty), and a tag click passes the tag so the same screen
 * opens showing that tag's results.
 *
 * All the request handling lives here so the screens can't drift apart:
 * last-write-wins cancellation, dedupe on append, min refresh duration, and
 * the rate-limit observation are defined once.
 */
abstract class WallpaperListViewModel(
    private val repository: WallpaperRepository,
    settingsRepository: SettingsRepository,
    private val errorMessage: (Throwable) -> String,
    private val initialQuery: String = "",
) : ViewModel() {

    protected val _uiState = MutableStateFlow(WallpaperListUiState())
    val uiState: StateFlow<WallpaperListUiState> = _uiState.asStateFlow()

    /** Minimum time the refresh indicator stays visible, so it can animate away. */
    private companion object {
        const val MIN_REFRESH_MS = 500L
    }

    /**
     * The single in-flight request. Starting a new first page cancels the
     * previous one so a slow response for an older query/filter can never
     * overwrite newer results (last-write-wins race).
     */
    private var loadJob: Job? = null

    init {
        // Seed the query synchronously so the search box already shows it on
        // the very first frame (e.g. the tag when opened from a detail screen).
        _uiState.update {
            it.copy(query = initialQuery, filters = it.filters.copy(query = initialQuery))
        }
        viewModelScope.launch {
            val settings = settingsRepository.current()
            _uiState.update {
                it.copy(
                    filters = WallhavenFilters(
                        categories = settings.categories,
                        sorting = settings.sorting,
                        orientation = settings.orientation,
                        query = initialQuery,
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
            val startedAt = SystemClock.elapsedRealtime()
            try {
                // forceRefresh bypasses the response cache so the user gets
                // live data, not a replay of the last fetch.
                repository.search(filters, 1, forceRefresh = true)
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
                // Keep the indicator up for at least MIN_REFRESH_MS so it has
                // time to animate away. Without this, a very fast network
                // round-trip can leave Material3's PullToRefreshBox stuck
                // showing the spinner (isRefreshing toggles true→false within
                // a single frame).
                val elapsed = SystemClock.elapsedRealtime() - startedAt
                if (elapsed < MIN_REFRESH_MS) delay(MIN_REFRESH_MS - elapsed)
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
}