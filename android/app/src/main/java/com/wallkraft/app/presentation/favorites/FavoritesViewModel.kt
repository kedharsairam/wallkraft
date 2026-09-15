package com.wallkraft.app.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallkraft.app.data.cache.OfflineImageStore
import com.wallkraft.app.data.prefs.RotationSettingsStore
import com.wallkraft.app.domain.model.Favorite
import com.wallkraft.app.domain.repository.CollectionsRepository
import com.wallkraft.app.domain.repository.FavoritesRepository
import com.wallkraft.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel for the Favorites tab — exposes the favorites list, rotation state, and collections. */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    val settingsRepository: SettingsRepository,
    val rotationStore: RotationSettingsStore,
    val collectionsRepository: CollectionsRepository,
    val favoriteImageStore: OfflineImageStore,
) : ViewModel() {

    val favorites: StateFlow<List<Favorite>> = favoritesRepository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun remove(id: String) {
        viewModelScope.launch { favoritesRepository.remove(id) }
    }
}
