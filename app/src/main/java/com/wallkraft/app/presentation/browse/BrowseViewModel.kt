package com.wallkraft.app.presentation.browse

import androidx.lifecycle.viewModelScope
import com.wallkraft.app.domain.repository.SettingsRepository
import com.wallkraft.app.domain.repository.WallpaperRepository
import com.wallkraft.app.presentation.common.WallpaperListViewModel
import com.wallkraft.app.util.toUserMessage
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The Browse tab. All pagination, refresh, and filter handling lives in
 * [WallpaperListViewModel]; this subclass only adds the search box.
 *
 * [initialQuery] seeds the search box and the API query. The tab passes ""
 * (empty); a tag click passes the tag, so the SAME screen opens showing that
 * tag's results — the tag appears in the search bar exactly like Wallhaven,
 * and the back stack keeps the detail screen (and the original Browse
 * position) beneath it.
 */
class BrowseViewModel(
    repository: WallpaperRepository,
    settingsRepository: SettingsRepository,
    errorMessage: (Throwable) -> String,
    initialQuery: String = "",
) : WallpaperListViewModel(repository, settingsRepository, errorMessage, initialQuery) {

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
}