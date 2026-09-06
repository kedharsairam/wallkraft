package com.wallkraft.app.data.repository

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
        val id = dao.insertCollection(
            CollectionEntity(name = cleaned, createdAt = System.currentTimeMillis()),
        )
        // IGNORE conflict (-1): resolve to the existing row.
        return if (id != -1L) id else dao.findIdByName(cleaned) ?: -1
    }

    override suspend fun rename(id: Long, name: String) {
        val cleaned = name.trim()
        if (cleaned.isNotEmpty()) dao.rename(id, cleaned)
    }

    override suspend fun delete(id: Long) {
        dao.deleteCollection(id)
    }

    override suspend fun addTo(collectionId: Long, wallpaperId: String) {
        dao.addItem(CollectionItemEntity(collectionId, wallpaperId))
    }

    override suspend fun removeFrom(collectionId: Long, wallpaperId: String) {
        dao.removeItem(collectionId, wallpaperId)
    }

    override fun observeIdsFor(wallpaperId: String): Flow<List<Long>> =
        dao.observeIdsFor(wallpaperId)
}
