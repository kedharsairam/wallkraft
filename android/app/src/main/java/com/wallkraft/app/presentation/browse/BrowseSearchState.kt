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
    /** Total result count for the current listing (0 = unknown). Shown in the search bar. */
    var totalResults by mutableStateOf(0)
    var hasApiKey by mutableStateOf(false)
    /** Explicit searches only (typed + submitted). Synced from SearchHistoryStore. */
    var history by mutableStateOf<List<String>>(emptyList())
    /** Set by BrowseScreen so the outer bar can trigger a search. */
    var onSearch: ((String) -> Unit)? = null
    /** Set by BrowseScreen so filter changes flow to the ViewModel. */
    var onFiltersChange: ((WallhavenFilters) -> Unit)? = null
    /** Set by BrowseScreen so the outer bar can clear history. */
    var onClearHistory: (() -> Unit)? = null
}
