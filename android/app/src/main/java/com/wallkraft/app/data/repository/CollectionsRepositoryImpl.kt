package com.wallkraft.app.data.repository

import android.database.sqlite.SQLiteConstraintException
import android.util.Log

import com.wallkraft.app.data.db.CollectionDao
import com.wallkraft.app.data.db.CollectionEntity
import com.wallkraft.app.data.db.CollectionItemEntity
import com.wallkraft.app.data.db.CollectionWithItems
import com.wallkraft.app.domain.repository.CollectionsRepository
import kotlinx.coroutines.flow.Flow

class CollectionsRepositoryImpl(
    private val dao: CollectionDao,
) : CollectionsRepository {

    override fun observeAll(): Flow<List<CollectionWithItems>> = dao.observeAll()

    override suspend fun create(name: String): Long {
        val cleaned = name.trim()
        if (cleaned.isEmpty()) return -1
        // Resolve first (case-insensitive): the DB unique index is
        // case-sensitive, so without this "Beach" + "beach" would fork.
        dao.findIdByName(cleaned)?.let { return it }
        val id = dao.insertCollection(
            CollectionEntity(name = cleaned, createdAt = System.currentTimeMillis()),
        )
        // IGNORE conflict (-1): exact-duplicate race — resolve to the winner.
        return if (id != -1L) id else dao.findIdByName(cleaned) ?: -1
    }

    override suspend fun rename(id: Long, name: String): Boolean {
        val cleaned = name.trim()
        if (cleaned.isEmpty()) return true
        // Taken by another collection (case-insensitive) — report, don't throw.
        if (dao.findIdByName(cleaned)?.let { it != id } == true) return false
        dao.rename(id, cleaned)
        return true
    }

    override suspend fun delete(id: Long) {
        dao.deleteCollection(id)
    }

    override suspend fun addTo(collectionId: Long, wallpaperId: String) {
        // Stale taps (deleted collection, unfavorited wallpaper) violate the
        // FK — ignore like duplicate adds instead of crashing. Callers
        // re-sync membership from the flow. Only FK violations are swallowed;
        // anything else still surfaces.
        try {
            dao.addItem(CollectionItemEntity(collectionId, wallpaperId))
        } catch (e: SQLiteConstraintException) {
            Log.w("CollectionsRepo", "addTo ignored (stale collection/member)", e)
        }
    }

    override suspend fun removeFrom(collectionId: Long, wallpaperId: String) {
        dao.removeItem(collectionId, wallpaperId)
    }

    override fun observeIdsFor(wallpaperId: String): Flow<List<Long>> =
        dao.observeIdsFor(wallpaperId)
}
