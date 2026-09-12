package com.wallkraft.app.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallkraft.app.core.errors.AppError
import com.wallkraft.app.core.utils.Result
import com.wallkraft.app.domain.model.Thumbs
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.repository.FavoritesRepository
import com.wallkraft.app.domain.repository.WallpaperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val wallpaper: Wallpaper? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    /** Set of favorite wallpaper IDs, so the fullscreen pager can mark each page. */
    val favoriteIds: Set<String> = emptySet(),
    /**
     * True once the detail API call has succeeded. The screen renders a grid
     * preview before that completes, and that preview has no uploader — this
     * flag is what lets the UI distinguish "still loading the uploader" from
     * "loaded, and there genuinely is no uploader (deleted account)".
     */
    val isDetailLoaded: Boolean = false,
)

/**
 * Detail screen VM — Hilt pilot.
 * Nav args "id", "thumb", "path" are read via [SavedStateHandle] so the Screen
 * no longer needs to pass them manually. A secondary constructor is retained for
 * unit tests (plain lambda + explicit id/thumbs without needing Hilt/SavedStateHandle).
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val favoritesRepository: FavoritesRepository,
    private val errorMessage: @JvmSuppressWildcards (AppError) -> String,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val id: String = savedStateHandle.get<String>("id") ?: ""
    private val previewThumb: String? = savedStateHandle.get<String>("thumb")?.takeIf { it.isNotBlank() }
    private val previewPath: String? = savedStateHandle.get<String>("path")?.takeIf { it.isNotBlank() }

    /**
     * Secondary constructor for unit tests — lets tests pass a plain lambda and
     * an explicit id/thumbs without needing a SavedStateHandle or Android resources.
     */
    constructor(
        id: String,
        wallpaperRepository: WallpaperRepository,
        favoritesRepository: FavoritesRepository,
        errorMessage: (AppError) -> String,
        previewThumb: String? = null,
        previewPath: String? = null,
    ) : this(
        wallpaperRepository = wallpaperRepository,
        favoritesRepository = favoritesRepository,
        errorMessage = errorMessage,
        savedStateHandle = SavedStateHandle(
            mapOf(
                "id" to id,
                "thumb" to (previewThumb ?: ""),
                "path" to (previewPath ?: ""),
            ),
        ),
    )

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
            _uiState.update { it.copy(isLoading = it.wallpaper == null, error = null) }
            when (val result = wallpaperRepository.wallpaper(id)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(wallpaper = result.data, isLoading = false, isDetailLoaded = true)
                    }
                }
                is Result.Failure -> {
                    // Keep the preview (if any) so the user still sees the
                    // image; only surface an error when there's nothing to show.
                    _uiState.update {
                        if (it.wallpaper != null) it.copy(isLoading = false)
                        else it.copy(isLoading = false, error = errorMessage(result.error))
                    }
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
