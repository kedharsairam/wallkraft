package com.wallkraft.app.presentation.browse

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.wallkraft.app.navigation.Browse
import com.wallkraft.app.core.errors.AppError
import com.wallkraft.app.data.prefs.SearchHistoryRepository
import com.wallkraft.app.domain.repository.SettingsRepository
import com.wallkraft.app.domain.repository.WallpaperRepository
import com.wallkraft.app.presentation.common.WallpaperListViewModel
import com.wallkraft.app.util.ElapsedClock
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.update

/**
 * The Browse tab. All pagination, refresh, and filter handling lives in
 * [WallpaperListViewModel]; this subclass only adds the search box.
 *
 * The nav arguments (query/title) seed the search box — the tab passes a default
 * [Browse]; a tag click passes a [Browse] with the tag via [SavedStateHandle] so the
 * SAME screen opens showing that tag's results.
 * The old manual constructor (errorMessage + initialQuery) is retained for unit tests via a
 * secondary constructor; production goes through the @Inject primary.
 */
@HiltViewModel
class BrowseViewModel @Inject constructor(
    wallpaperRepository: WallpaperRepository,
    settingsRepository: SettingsRepository,
    val searchHistoryStore: SearchHistoryRepository,
    errorMessageMapper: @JvmSuppressWildcards (AppError) -> String,
    savedStateHandle: SavedStateHandle,
    clock: ElapsedClock,
) : WallpaperListViewModel(
    repository = wallpaperRepository,
    settingsRepository = settingsRepository,
    errorMessage = errorMessageMapper,
    initialQuery = savedStateHandle.toRoute<Browse>().query,
    clock = clock,
) {

    /**
     * Secondary constructor for unit tests — lets tests pass a plain lambda and an explicit
     * query without needing a SavedStateHandle or Android resources.
     *
     * Note: the handle mirrors what NavController stores for a typed [Browse]
     * (all args present). Plain JVM unit tests stub android.os.Bundle, which
     * navigation's toRoute decodes through, so only the default ("") query is
     * meaningful here — custom queries must be driven via [search].
     */
    constructor(
        repository: WallpaperRepository,
        settingsRepository: SettingsRepository,
        searchHistoryStore: SearchHistoryRepository,
        errorMessage: (AppError) -> String,
        initialQuery: String = "",
        clock: ElapsedClock = ElapsedClock { android.os.SystemClock.elapsedRealtime() },
    ) : this(
        wallpaperRepository = repository,
        settingsRepository = settingsRepository,
        searchHistoryStore = searchHistoryStore,
        errorMessageMapper = errorMessage,
        savedStateHandle = SavedStateHandle(mapOf("query" to initialQuery, "title" to "")),
        clock = clock,
    )

    fun search(newQuery: String) {
        _uiState.update {
            it.copy(
                filters = it.filters.copy(query = newQuery),
                // A new query replaces the whole list: drop the old results so
                // a failure shows the error state instead of silently keeping
                // stale wallpapers from the previous search.
                wallpapers = emptyList(),
            )
        }
        loadFirstPage()
    }
}
