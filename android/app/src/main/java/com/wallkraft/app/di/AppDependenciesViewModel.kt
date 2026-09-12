package com.wallkraft.app.di

import androidx.lifecycle.ViewModel
import com.wallkraft.app.data.cache.FavoriteImageStore
import com.wallkraft.app.data.prefs.RotationCropStore
import com.wallkraft.app.data.prefs.RotationStore
import com.wallkraft.app.data.prefs.SearchHistoryStore
import com.wallkraft.app.domain.repository.CollectionsRepository
import com.wallkraft.app.domain.repository.FavoritesRepository
import com.wallkraft.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Shared Hilt ViewModel that exposes singleton stores needed by composables
 * that previously used [com.wallkraft.app.AppContainer].
 *
 * Each screen pulls only what it needs via `hiltViewModel<AppDependenciesViewModel>()`
 * so Browse/Favorites/Detail/Settings no longer take a `container: AppContainer` param.
 * The underlying stores are @Singleton so every screen shares the same DataStore files.
 */
@HiltViewModel
class AppDependenciesViewModel @Inject constructor(
    val settingsRepository: SettingsRepository,
    val searchHistoryStore: SearchHistoryStore,
    val rotationStore: RotationStore,
    val rotationCropStore: RotationCropStore,
    val favoriteImageStore: FavoriteImageStore,
    val collectionsRepository: CollectionsRepository,
    val favoritesRepository: FavoritesRepository,
) : ViewModel()
