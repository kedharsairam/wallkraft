package com.wallkraft.app.presentation.browse

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wallkraft.app.domain.model.WallhavenFilters

/**
 * Shared search state between the outer Scaffold (SearchFilterBar) and
 * BrowseScreen content. Lives outside the SharedTransitionLayout so the
 * top bar is never eclipsed by the shared element overlay.
 */
class BrowseSearchState {
    var query by mutableStateOf("")
    var titleActive by mutableStateOf(false)
    var filters by mutableStateOf(WallhavenFilters())
    var hasApiKey by mutableStateOf(false)
    /** Set by BrowseScreen so the outer bar can trigger a search. */
    var onSearch: ((String) -> Unit)? = null
    /** Set by BrowseScreen so filter changes flow to the ViewModel. */
    var onFiltersChange: ((WallhavenFilters) -> Unit)? = null
}
