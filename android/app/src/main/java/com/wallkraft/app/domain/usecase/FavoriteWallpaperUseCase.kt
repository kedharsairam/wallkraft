/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Kedhar Sairam
 */
package com.wallkraft.app.domain.usecase

import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.repository.CollectionsRepository
import com.wallkraft.app.domain.repository.FavoritesRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages favorite and collection operations for wallpapers.
 *
 * Encapsulates the business logic that was formerly inline in ViewModels
 * (toggleFavorite, addToCollection, removeFromCollection). The class is
 * injectable and the repositories are replaceable for testing.
 */
@Singleton
class FavoriteWallpaperUseCase @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val collectionsRepository: CollectionsRepository,
) {

    /**
     * Toggles the favorite state of [wallpaper].
     *
     * If the wallpaper is already a favorite it is removed; otherwise it is
     * added. Returns `true` when the wallpaper ends up as a favorite.
     */
    suspend fun toggleFavorite(wallpaper: Wallpaper): Boolean {
        return if (favoritesRepository.isFavorite(wallpaper.id)) {
            favoritesRepository.remove(wallpaper.id)
            false
        } else {
            favoritesRepository.add(wallpaper)
            true
        }
    }

    /**
     * Adds [wallpaperId] to the given collection.
     *
     * Duplicate adds are silently ignored. If the collection has been deleted
     * or the wallpaper isn't favorited, this is a safe no-op.
     */
    suspend fun addToCollection(collectionId: Long, wallpaperId: String) {
        collectionsRepository.addTo(collectionId, wallpaperId)
    }

    /**
     * Removes [wallpaperId] from the given collection.
     */
    suspend fun removeFromCollection(collectionId: Long, wallpaperId: String) {
        collectionsRepository.removeFrom(collectionId, wallpaperId)
    }
}
