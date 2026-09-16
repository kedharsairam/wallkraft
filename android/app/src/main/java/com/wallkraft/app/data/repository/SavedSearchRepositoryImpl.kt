/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Kedhar Sairam
 */
package com.wallkraft.app.data.repository

import com.wallkraft.app.data.db.ORIENTATION_BOTH
import com.wallkraft.app.data.db.SavedSearchDao
import com.wallkraft.app.data.db.SavedSearchEntity
import com.wallkraft.app.data.mappers.toDomain
import com.wallkraft.app.domain.model.SavedSearch
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.toCategoryParam
import com.wallkraft.app.domain.model.toPurityParam
import com.wallkraft.app.domain.repository.SavedSearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SavedSearchRepositoryImpl(
    private val dao: SavedSearchDao,
) : SavedSearchRepository {

    override fun observeAll(): Flow<List<SavedSearch>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun save(name: String, filters: WallhavenFilters): Long {
        val cleaned = name.trim()
        if (cleaned.isEmpty()) return -1
        val now = System.currentTimeMillis()
        // Resolve first (case-insensitive): the DB unique index is
        // case-sensitive, so without this "Blue" + "blue" would fork.
        // Re-saving overwrites the filters (last write wins) and counts as a use.
        dao.findIdByName(cleaned)?.let { existing ->
            dao.updateFilters(
                id = existing,
                query = filters.query,
                categories = filters.categories.toCategoryParam(),
                purity = filters.purity.toPurityParam(),
                sorting = filters.sorting.value,
                topRange = filters.topRange.value,
                colors = filters.colors,
                orientation = filters.orientation.value.ifBlank { ORIENTATION_BOTH },
                now = now,
            )
            return existing
        }
        val id = dao.insert(SavedSearchEntity.fromFilters(cleaned, filters, now))
        // IGNORE conflict (-1): exact-duplicate race — resolve to the winner.
        return if (id != -1L) id else dao.findIdByName(cleaned) ?: -1
    }

    override suspend fun rename(id: Long, name: String): Boolean {
        val cleaned = name.trim()
        if (cleaned.isEmpty()) return true
        // Taken by another search (case-insensitive) — report, don't throw.
        if (dao.findIdByName(cleaned)?.let { it != id } == true) return false
        dao.rename(id, cleaned)
        return true
    }

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun recordUse(id: Long) {
        dao.touch(id, System.currentTimeMillis())
    }
}
