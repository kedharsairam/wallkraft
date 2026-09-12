package com.wallkraft.app.domain.model

import com.wallkraft.app.data.db.CollectionWithItems

/**
 * Pure domain representation of a user-created favorites collection.
 * Decouples presentation/domain from Room's [CollectionWithItems].
 */
data class Collection(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val items: List<String> = emptyList(),
)

/** Maps a Room [CollectionWithItems] to its pure domain [Collection]. */
fun CollectionWithItems.toDomain(): Collection = Collection(
    id = collection.id,
    name = collection.name,
    createdAt = collection.createdAt,
    items = items.map { it.wallpaperId },
)
