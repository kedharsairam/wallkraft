package com.wallkraft.app.data.mappers

import com.wallkraft.app.data.db.CollectionWithItems
import com.wallkraft.app.domain.model.Collection

/** Maps a Room [CollectionWithItems] to its pure domain [Collection]. */
fun CollectionWithItems.toDomain(): Collection = Collection(
    id = collection.id,
    name = collection.name,
    createdAt = collection.createdAt,
    items = items.map { it.wallpaperId },
)
