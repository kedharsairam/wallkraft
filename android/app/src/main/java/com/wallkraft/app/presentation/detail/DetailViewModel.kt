package com.wallkraft.app.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallkraft.app.domain.model.Thumbs
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.repository.FavoritesRepository
import com.wallkraft.app.domain.repository.WallpaperRepository
import com.wallkraft.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class DetailUiState(
    val wallpaper: Wallpaper? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    /** Set of favorite wallpaper IDs, so the fullscreen pager can mark each page. */
    val favoriteIds: Set<String> = emptySet(),
)

class DetailViewModel(
    private val id: String,
    private val wallpaperRepository: WallpaperRepository,
    private val favoritesRepository: FavoritesRepository,
    private val errorMessage: (Throwable) -> String,
    previewThumb: String? = null,
    previewPath: String? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        // Seed a preview wallpaper from the grid so the image renders
        // instantly (no spinner) while the full metadata loads in the
        // background. The preview carries the thumbnail and full-res path the
        // grid already has; the API call then enriches it with tags, file
        // size, exact dimensions, etc. Only seed when we have a path — the
        // detail screen needs it to render the image at all.
        if (!previewPath.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    wallpaper = Wallpaper(
                        id = id,
                        path = previewPath,
                        thumbs = Thumbs(original = previewThumb),
                    ),
                    isLoading = false,
                )
            }
        }
        viewModelScope.launch {
            favoritesRepository.observeAll().collect { favorites ->
                _uiState.update {
                    it.copy(
                        favoriteIds = favorites.map { f -> f.wallpaper.id }.toSet(),
                    )
                }
            }
        }
        load()
    }

    fun load() {
        // Cancel any in-flight load so a stale response can't overwrite a
        // newer one (mirrors the search-race guard in BrowseViewModel).
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // Only show the spinner when there's nothing on screen yet (no
            // preview). With a preview, the image is already visible, so the
            // background refresh must not flash a spinner over it.
            _uiState.update { it.copy(isLoading = _uiState.value.wallpaper == null, error = null) }
            wallpaperRepository.wallpaper(id)
                .onSuccess { wallpaper ->
                    _uiState.update { it.copy(wallpaper = wallpaper, isLoading = false) }
                }
                .onFailure { e ->
                    // Keep the preview (if any) so the user still sees the
                    // image; only surface an error when there's nothing to show.
                    _uiState.update {
                        if (it.wallpaper != null) it.copy(isLoading = false)
                        else it.copy(isLoading = false, error = errorMessage(e))
                    }
                }
        }
    }

    /** Toggles favorite state for [wallpaper] — any wallpaper in the pager. */
    fun toggleFavorite(wallpaper: Wallpaper) {
        viewModelScope.launch {
            if (wallpaper.id in _uiState.value.favoriteIds) {
                favoritesRepository.remove(wallpaper.id)
            } else {
                favoritesRepository.add(wallpaper)
            }
        }
    }
}
