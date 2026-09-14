package com.wallkraft.app.data.mappers

import com.wallkraft.app.data.db.SavedSearchEntity
import com.wallkraft.app.domain.model.SavedSearch

/** Maps a Room [SavedSearchEntity] to its pure domain [SavedSearch]. */
fun SavedSearchEntity.toDomain(): SavedSearch = SavedSearch(
    id = id,
    name = name,
    filters = toFilters(),
    createdAt = createdAt,
    lastUsedAt = lastUsedAt,
    useCount = useCount,
)
