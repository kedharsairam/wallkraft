package com.wallkraft.app.domain.repository

import com.wallkraft.app.domain.model.Wallpaper
import kotlinx.coroutines.flow.Flow

/** A wallpaper the user has favorited, persisted locally. */
data class Favorite(
    val wallpaper: Wallpaper,
    val addedAt: Long,
)

interface FavoritesRepository {
    fun observeAll(): Flow<List<Favorite>>
    suspend fun isFavorite(id: String): Boolean
    suspend fun add(wallpaper: Wallpaper)
    suspend fun remove(id: String)
}
