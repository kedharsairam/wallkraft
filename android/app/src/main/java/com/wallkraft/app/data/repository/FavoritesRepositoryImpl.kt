package com.wallkraft.app.data.repository

import com.wallkraft.app.data.db.FavoriteDao
import com.wallkraft.app.data.db.FavoriteEntity
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.model.Favorite
import com.wallkraft.app.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class FavoritesRepositoryImpl(
    private val dao: FavoriteDao,
    private val json: Json,
) : FavoritesRepository {

    override fun observeAll(): Flow<List<Favorite>> =
        dao.observeAll().map { entities ->
            entities.map { Favorite(it.toWallpaper(json), it.addedAt) }
        }

    override suspend fun isFavorite(id: String): Boolean = dao.exists(id)

    override suspend fun add(wallpaper: Wallpaper) {
        dao.upsert(
            FavoriteEntity.fromWallpaper(
                wallpaper,
                System.currentTimeMillis(),
                json,
            ),
        )
    }

    override suspend fun remove(id: String) {
        dao.deleteById(id)
    }
}
