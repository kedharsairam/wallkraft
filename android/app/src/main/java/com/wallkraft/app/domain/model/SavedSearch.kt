package com.wallkraft.app.domain.model

/**
 * Pure domain representation of a user-saved Browse filter set.
 * Decouples presentation/domain from Room's [SavedSearchEntity].
 */
data class SavedSearch(
    val id: Long,
    val name: String,
    val filters: WallhavenFilters,
    val createdAt: Long,
    val lastUsedAt: Long,
    val useCount: Int = 0,
)
