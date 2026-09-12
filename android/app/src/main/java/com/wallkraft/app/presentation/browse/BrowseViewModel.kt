package com.wallkraft.app.presentation.browse

import androidx.lifecycle.SavedStateHandle
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
 * Hilt pilot: this is the first ViewModel migrated off [com.wallkraft.app.AppContainer].
 * The nav argument "query" seeds the search box — the tab passes "" (empty); a tag click
 * passes the tag via [SavedStateHandle] so the SAME screen opens showing that tag's results.
 * The old manual constructor (errorMessage + initialQuery) is retained for unit tests via a
 * secondary constructor; production goes through the @Inject primary.
 */
@HiltViewModel
class BrowseViewModel @Inject constructor(
    wallpaperRepository: WallpaperRepository,
    settingsRepository: SettingsRepository,
    errorMessageMapper: @JvmSuppressWildcards (Throwable) -> String,
    savedStateHandle: SavedStateHandle,
    clock: ElapsedClock,
) : WallpaperListViewModel(
    repository = wallpaperRepository,
    settingsRepository = settingsRepository,
    errorMessage = errorMessageMapper,
    initialQuery = savedStateHandle.get<String>("query") ?: "",
    clock = clock,
) {

    /**
     * Secondary constructor for unit tests — lets tests pass a plain lambda and an explicit
     * query without needing a SavedStateHandle or Android resources.
     */
    constructor(
        repository: WallpaperRepository,
        settingsRepository: SettingsRepository,
        errorMessage: (Throwable) -> String,
        initialQuery: String = "",
        clock: ElapsedClock = ElapsedClock { android.os.SystemClock.elapsedRealtime() },
    ) : this(
        wallpaperRepository = repository,
        settingsRepository = settingsRepository,
        errorMessageMapper = errorMessage,
        savedStateHandle = SavedStateHandle(mapOf("query" to initialQuery)),
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
