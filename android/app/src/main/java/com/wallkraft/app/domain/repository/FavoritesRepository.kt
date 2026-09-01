package com.wallkraft.app.domain.repository

import com.wallkraft.app.domain.model.Favorite
import com.wallkraft.app.domain.model.Wallpaper
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun observeAll(): Flow<List<Favorite>>
    suspend fun isFavorite(id: String): Boolean
    suspend fun add(wallpaper: Wallpaper)
    suspend fun remove(id: String)
}
