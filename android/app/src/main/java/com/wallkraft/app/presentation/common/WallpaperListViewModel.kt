package com.wallkraft.app.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallkraft.app.core.design.KraftConstants
import com.wallkraft.app.core.errors.AppError
import com.wallkraft.app.core.utils.Result
import com.wallkraft.app.domain.model.Category
import com.wallkraft.app.domain.model.Purity
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.repository.SettingsRepository
import com.wallkraft.app.domain.repository.WallpaperRepository
import com.wallkraft.app.util.ElapsedClock
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
    val currentPage: Int = 1,
    val lastPage: Int = 1,
    val hasMore: Boolean = true,
    /** Total result count from the API (meta.total). 0 = unknown. */
    val totalResults: Int = 0,
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
    private val errorMessage: (AppError) -> String,
    private val initialQuery: String = "",
    private val clock: ElapsedClock = ElapsedClock { android.os.SystemClock.elapsedRealtime() },
) : ViewModel() {

    protected val _uiState = MutableStateFlow(WallpaperListUiState())
    val uiState: StateFlow<WallpaperListUiState> = _uiState.asStateFlow()

    /** Minimum time the refresh indicator stays visible, so it can animate away. */
    private companion object {
        const val MIN_REFRESH_MS = KraftConstants.MinRefreshMs
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
            it.copy(filters = it.filters.copy(query = initialQuery))
        }
        viewModelScope.launch {
            val settings = settingsRepository.current()
            // Tag/uploader searches (initialQuery non-blank from navigation) should
            // show all categories like wallhaven.cc does — otherwise a People tag
            // with a General-only filter returns empty. Search-bar queries keep
            // the user's chosen categories.
            val initialCategories = if (initialQuery.isNotBlank()) {
                setOf(Category.General, Category.Anime, Category.People)
            } else {
                settings.categories
            }
            // NSFW requires a valid API key — strip it if no key is set or invalid.
            val effectivePurity = if (settings.apiKeyValid) {
                settings.purity
            } else {
                settings.purity - Purity.NSFW
            }
            _uiState.update {
                it.copy(
                    filters = WallhavenFilters(
                        categories = initialCategories,
                        purity = effectivePurity,
                        sorting = settings.sorting,
                        topRange = settings.topRange,
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
                // Preserve the current query — the filter sheet only changes
                // purity/sorting/orientation/category, never the search text.
                filters = filters.copy(query = it.filters.query),
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
            val startedAt = clock.elapsedMs()
            try {
                // forceRefresh bypasses the response cache so the user gets
                // live data, not a replay of the last fetch.
                when (val result = repository.search(filters, 1, forceRefresh = true)) {
                    is Result.Success -> {
                        val response = result.data
                        _uiState.update {
                            it.copy(
                                wallpapers = response.data,
                                isRefreshing = false,
                                currentPage = response.meta.currentPage,
                                lastPage = response.meta.lastPage,
                                hasMore = response.meta.currentPage < response.meta.lastPage,
                                totalResults = response.meta.total,
                                error = null,
                            )
                        }
                    }
                    is Result.Failure -> {
                        _uiState.update {
                            it.copy(isRefreshing = false, error = errorMessage(result.error))
                        }
                    }
                }
            } finally {
                // Keep the indicator up for at least MIN_REFRESH_MS so it has
                // time to animate away. Without this, a very fast network
                // round-trip can leave Material3's PullToRefreshBox stuck
                // showing the spinner (isRefreshing toggles true→false within
                // a single frame).
                val elapsed = clock.elapsedMs() - startedAt
                if (elapsed < MIN_REFRESH_MS) delay(MIN_REFRESH_MS - elapsed)
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun loadFirstPage() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // Reset the total so the search-bar count hides until the new
            // query's count arrives (never show a stale total).
            _uiState.update { it.copy(isInitialLoading = true, error = null, totalResults = 0) }
            val filters = _uiState.value.filters
            try {
                when (val result = repository.search(filters, 1)) {
                    is Result.Success -> {
                        val response = result.data
                        _uiState.update {
                            it.copy(
                                wallpapers = response.data,
                                isInitialLoading = false,
                                isAppending = false,
                                currentPage = response.meta.currentPage,
                                lastPage = response.meta.lastPage,
                                hasMore = response.meta.currentPage < response.meta.lastPage,
                                totalResults = response.meta.total,
                                error = null,
                            )
                        }
                    }
                    is Result.Failure -> {
                        _uiState.update {
                            it.copy(isInitialLoading = false, error = errorMessage(result.error))
                        }
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
                when (val result = repository.search(state.filters, state.currentPage + 1)) {
                    is Result.Success -> {
                        val response = result.data
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
                                totalResults = response.meta.total,
                            )
                        }
                    }
                    is Result.Failure -> {
                        _uiState.update {
                            it.copy(isAppending = false, error = errorMessage(result.error))
                        }
                    }
                }
            } finally {
                _uiState.update { it.copy(isAppending = false) }
            }
        }
    }
}
