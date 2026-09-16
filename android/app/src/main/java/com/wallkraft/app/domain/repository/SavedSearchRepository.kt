/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Kedhar Sairam
 */
package com.wallkraft.app.domain.repository

import com.wallkraft.app.domain.model.SavedSearch
import com.wallkraft.app.domain.model.WallhavenFilters
import kotlinx.coroutines.flow.Flow

/**
 * User-saved Browse filter sets ("Blue anime", "Toplist month", …).
 *
 * This is the reusable-filter store (explicit filter columns, ordered by
 * recency). The DataStore query-string history stays as-is for dumb recents.
 * Names are unique — saving a duplicate overwrites its filters and resolves
 * to the existing row.
 */
interface SavedSearchRepository {
    /** All saved searches, most-recently-used first. */
    fun observeAll(): Flow<List<SavedSearch>>

    /**
     * Saves [filters] under [name]. Returns the id, or the existing id when
     * [name] already exists (matched case-insensitively) — the existing row's
     * filters are overwritten. Returns -1 for blank names.
     */
    suspend fun save(name: String, filters: WallhavenFilters): Long

    /**
     * Renames a saved search. Returns false when [name] is taken by ANOTHER
     * search (case-insensitive) — the row is left untouched. Blank names are
     * ignored and report success. Never throws for duplicates.
     */
    suspend fun rename(id: Long, name: String): Boolean
    suspend fun delete(id: Long)

    /** Records a use (Browse apply, rotation pick): bumps recency + count. */
    suspend fun recordUse(id: Long)
}
