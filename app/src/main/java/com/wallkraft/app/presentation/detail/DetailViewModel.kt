package com.wallkraft.app.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.repository.FavoritesRepository
import com.wallkraft.app.domain.repository.WallpaperRepository
import com.wallkraft.app.presentation.browse.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val wallpaper: Wallpaper? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isFavorite: Boolean = false,
)

class DetailViewModel(
    private val id: String,
    private val wallpaperRepository: WallpaperRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            favoritesRepository.observeAll().collect { favorites ->
                _uiState.update { it.copy(isFavorite = favorites.any { f -> f.wallpaper.id == id }) }
            }
        }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            wallpaperRepository.wallpaper(id)
                .onSuccess { wallpaper ->
                    _uiState.update { it.copy(wallpaper = wallpaper, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.toUserMessage()) }
                }
        }
    }

    fun toggleFavorite() {
        val wallpaper = _uiState.value.wallpaper ?: return
        viewModelScope.launch {
            if (_uiState.value.isFavorite) {
                favoritesRepository.remove(id)
            } else {
                favoritesRepository.add(wallpaper)
            }
        }
    }
}
