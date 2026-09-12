package com.wallkraft.app.domain.model

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
