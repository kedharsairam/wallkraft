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
import com.wallkraft.app.domain.model.WallpaperResponse
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
    /**
     * Wall-clock time (epoch millis) the visible first page was fetched.
     * Null on fresh network data that carries no timestamp yet and while a
     * new query loads. The Browse header shows "Updated X ago" only when this
     * is older than the search-cache TTL.
     */
    val cachedAt: Long? = null,
    /**
     * When non-null, the displayed results were fetched with a degraded filter
     * set because the user's original filters returned empty. Null means the
     * shown results match the user's requested filters exactly.
     */
    val appliedFilters: WallhavenFilters? = null,
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
    val settingsRepository: SettingsRepository,
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
     * The single in-flight request for first-page loads only. Starting a new
     * first page cancels the previous one so a slow response for an older
     * query/filter can never overwrite newer results (last-write-wins race).
     * Page appends are NOT cancelled — the distinctBy { id } dedup handles
     * duplicates safely, and cancelling during fast scroll drops results.
     */
    private var loadJob: Job? = null

    /**
     * Set by subclasses in their init block before calling [loadFirstPage] to
     * prevent the parent coroutine from overwriting the child's filter
     * configuration. The parent's init launches an async coroutine that reads
     * settings and updates filters — without this guard, the child's filters
     * get stomped before loadFirstPage runs.
     */
    protected var filtersConfigured = false

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
            if (!filtersConfigured) {
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

    /** Clear fallback banner — re-run with the user's original filters (showing true empty state). */
    fun showOriginalFilters() {
        _uiState.update { it.copy(appliedFilters = null) }
        loadFirstPage()
    }

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
                                currentPage = response.meta.currentPage,
                                lastPage = response.meta.lastPage,
                                hasMore = response.meta.currentPage < response.meta.lastPage,
                                totalResults = response.meta.total,
                                cachedAt = response.cachedAt,
                                error = null,
                            )
                        }
                    }
                    is Result.Failure -> {
                        _uiState.update {
                            it.copy(error = errorMessage(result.error))
                        }
                    }
                }
            } finally {
                // Keep the indicator up for at least MIN_REFRESH_MS so it has
                // time to animate away. Without this, a very fast network
                // round-trip can leave Material3's PullToRefreshBox stuck
                // showing the spinner (isRefreshing toggles true→false within
                // a single frame). Note: Success/Failure above do NOT clear
                // isRefreshing — that happens here after the minimum delay.
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
            _uiState.update { it.copy(isInitialLoading = true, error = null, totalResults = 0, cachedAt = null) }
            val filters = _uiState.value.filters
            try {
                when (val result = repository.search(filters, 1)) {
                    is Result.Success -> {
                        val response = result.data
                        if (response.data.isEmpty() && !_uiState.value.rateLimited) {
                            val fallbackResult = tryPurityFallback(filters)
                            if (fallbackResult != null) {
                                val resp = fallbackResult.response
                                _uiState.update {
                                    it.copy(
                                        wallpapers = resp.data,
                                        isInitialLoading = false,
                                        isAppending = false,
                                        currentPage = resp.meta.currentPage,
                                        lastPage = resp.meta.lastPage,
                                        hasMore = resp.meta.currentPage < resp.meta.lastPage,
                                        totalResults = resp.meta.total,
                                        cachedAt = resp.cachedAt,
                                        error = null,
                                        appliedFilters = fallbackResult.filters,
                                    )
                                }
                            } else {
                                _uiState.update {
                                    it.copy(
                                        wallpapers = emptyList(),
                                        isInitialLoading = false,
                                        isAppending = false,
                                        currentPage = response.meta.currentPage,
                                        lastPage = response.meta.lastPage,
                                        hasMore = false,
                                        totalResults = 0,
                                        cachedAt = null,
                                        error = null,
                                    )
                                }
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    wallpapers = response.data,
                                    isInitialLoading = false,
                                    isAppending = false,
                                    currentPage = response.meta.currentPage,
                                    lastPage = response.meta.lastPage,
                                    hasMore = response.meta.currentPage < response.meta.lastPage,
                                    totalResults = response.meta.total,
                                    cachedAt = response.cachedAt,
                                    error = null,
                                )
                            }
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

    /**
     * Tries progressively looser purity/category combinations when the user's
     * original filters return zero results. Returns the first non-empty
     * [WallpaperResponse] or null if all fallbacks are also empty.
     *
     * Fallback sequence (at most 3 extra API calls):
     * 1. current categories + SFW+Sketchy (drop NSFW)
     * 2. current categories + SFW-only
     * 3. all categories + SFW-only
     *
     * Skips NSFW rung if the original filters didn't include NSFW. Never
     * triggers when purity is already maximally permissive AND categories are
     * all three — there's nothing more permissive to try.
     */
    private suspend fun tryPurityFallback(
        originalFilters: WallhavenFilters,
    ): FallbackResult? {
        val originalPurity = originalFilters.purity
        val originalCategories = originalFilters.categories

        val allPurity = setOf(Purity.SFW, Purity.Sketchy, Purity.NSFW)
        val allCategories = setOf(Category.General, Category.Anime, Category.People)

        if (originalPurity == allPurity && originalCategories == allCategories) return null

        val fallbackCandidates = buildList {
            // Rung 1: relax purity — add Sketchy if original was SFW-only
            if (Purity.Sketchy !in originalPurity) {
                add(
                    originalFilters.copy(
                        purity = originalPurity + Purity.Sketchy,
                    )
                )
            }
            // Rung 2: relax categories — try all categories with same purity
            if (originalCategories != allCategories) {
                add(
                    originalFilters.copy(
                        categories = allCategories,
                    )
                )
            }
            // Rung 3: relax both purity and categories (SFW+Sketchy + all categories)
            if (Purity.Sketchy !in originalPurity && originalCategories != allCategories) {
                add(
                    originalFilters.copy(
                        purity = setOf(Purity.SFW, Purity.Sketchy),
                        categories = allCategories,
                    )
                )
            }
        }

        for (candidate in fallbackCandidates) {
            val result = repository.search(candidate, 1)
            if (result is Result.Success && result.data.data.isNotEmpty()) {
                return FallbackResult(result.data, candidate)
            }
            if (result is Result.Failure) break
        }
        return null
    }

    private class FallbackResult(
        val response: WallpaperResponse,
        val filters: WallhavenFilters,
    )

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isInitialLoading || state.isAppending || !state.hasMore) return
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
