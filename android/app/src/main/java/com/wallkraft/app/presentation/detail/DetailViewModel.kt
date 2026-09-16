/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Kedhar Sairam
 */
package com.wallkraft.app.presentation.detail

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.wallkraft.app.core.errors.AppError
import com.wallkraft.app.core.utils.Result
import com.wallkraft.app.data.cache.OfflineImageStore
import com.wallkraft.app.data.db.WallpaperHistoryDao
import com.wallkraft.app.data.db.WallpaperHistoryEntity
import com.wallkraft.app.data.prefs.CropStore
import com.wallkraft.app.domain.model.Thumbs
import com.wallkraft.app.domain.model.Wallpaper
import com.wallkraft.app.domain.model.WallpaperResponse
import com.wallkraft.app.domain.repository.FavoritesRepository
import com.wallkraft.app.domain.repository.SettingsRepository
import com.wallkraft.app.domain.repository.WallpaperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    /** Extracted palette swatches from the wallpaper bitmap. */
    val palette: List<Int> = emptyList(),
    /** Related wallpapers (like:$id), capped at ~20, self excluded. */
    val related: List<Wallpaper> = emptyList(),
    val relatedLoading: Boolean = false,
)

/**
 * Detail screen VM — Hilt pilot.
 * Nav args "id", "thumb", "path" are read via [SavedStateHandle] so the Screen
 * no longer needs to pass them manually. Direct reads (instead of
 * SavedStateHandle.toRoute) keep plain JVM unit tests working: navigation
 * decodes through android.os.Bundle, which is a no-op stub under unit tests.
 * A secondary constructor is retained for unit tests (plain lambda + explicit
 * id/thumbs without needing Hilt/SavedStateHandle).
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val favoritesRepository: FavoritesRepository,
    val settingsRepository: SettingsRepository,
    val favoriteImageStore: OfflineImageStore,
    val rotationCropStore: CropStore,
    private val wallpaperHistoryDao: WallpaperHistoryDao,
    private val errorMessage: @JvmSuppressWildcards (AppError) -> String,
    @ApplicationContext private val appContext: Context,
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
        settingsRepository: SettingsRepository,
        favoriteImageStore: OfflineImageStore,
        rotationCropStore: CropStore,
        wallpaperHistoryDao: WallpaperHistoryDao,
        errorMessage: (AppError) -> String,
        previewThumb: String? = null,
        previewPath: String? = null,
    ) : this(
        wallpaperRepository = wallpaperRepository,
        favoritesRepository = favoritesRepository,
        settingsRepository = settingsRepository,
        favoriteImageStore = favoriteImageStore,
        rotationCropStore = rotationCropStore,
        wallpaperHistoryDao = wallpaperHistoryDao,
        errorMessage = errorMessage,
        appContext = android.app.Application(),
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
    private var relatedJob: Job? = null

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
                    extractPalette(result.data)
                    loadRelated(result.data.id)
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

    /**
     * Loads related wallpapers using Wallhaven's "like:" query.
     * Replaces the current wallpaper in-place (no navigation push).
     */
    fun swapToRelated(wallpaper: Wallpaper) {
        _uiState.update { it.copy(wallpaper = wallpaper, isDetailLoaded = true) }
        extractPalette(wallpaper)
        loadRelated(wallpaper.id)
    }

    /**
     * Extract dominant palette from the wallpaper bitmap.
     * Downscales to max 112px to avoid memory pressure and Hardware bitmap
     * crashes (Palette cannot read Hardware configs).
     */
    private fun extractPalette(wallpaper: Wallpaper) {
        val url = wallpaper.path ?: return
        viewModelScope.launch {
            try {
                // Single fetch: read the bytes once, then decode bounds + bitmap
                // from memory. The old code opened the URL stream twice (two
                // full downloads) — wasteful on slow/metered connections.
                val bitmap = withContext(Dispatchers.IO) {
                    val bytes = URL(url).openStream().use { it.readBytes() }
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    var inSampleSize = 1
                    while (bounds.outWidth / inSampleSize > 112 || bounds.outHeight / inSampleSize > 112) {
                        inSampleSize *= 2
                    }
                    val opts = BitmapFactory.Options().apply {
                        inJustDecodeBounds = false
                        this.inSampleSize = inSampleSize
                    }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                }
                if (bitmap != null && !bitmap.isRecycled) {
                    val palette = withContext(Dispatchers.Default) {
                        Palette.from(bitmap).generate()
                    }
                    val swatches = buildList {
                        palette.dominantSwatch?.let { add(it.rgb) }
                        palette.vibrantSwatch?.let { add(it.rgb) }
                        palette.mutedSwatch?.let { add(it.rgb) }
                        palette.darkVibrantSwatch?.let { add(it.rgb) }
                        palette.darkMutedSwatch?.let { add(it.rgb) }
                        palette.lightVibrantSwatch?.let { add(it.rgb) }
                        palette.lightMutedSwatch?.let { add(it.rgb) }
                    }.distinct()
                    bitmap.recycle()
                    if (swatches.isNotEmpty()) {
                        _uiState.update { it.copy(palette = swatches) }
                    }
                }
            } catch (_: Exception) {
                // Palette extraction is non-critical — silently ignore failures
            }
        }
    }

    fun recordHistory(wallpaper: Wallpaper, source: String) {
        viewModelScope.launch {
            wallpaperHistoryDao.insert(
                WallpaperHistoryEntity(
                    wallpaperId = wallpaper.id,
                    path = wallpaper.path,
                    thumbnail = wallpaper.thumbnail.orEmpty(),
                    setAt = System.currentTimeMillis(),
                    source = source,
                ),
            )
        }
    }

    /**
     * Loads related wallpapers using the "like:" query.
     * Caps at ~20 results and filters out the current wallpaper.
     */
    private fun loadRelated(wallpaperId: String) {
        relatedJob?.cancel()
        relatedJob = viewModelScope.launch {
            _uiState.update { it.copy(relatedLoading = true) }
            val filters = com.wallkraft.app.domain.model.WallhavenFilters(
                query = "like:$wallpaperId",
            )
            when (val result = wallpaperRepository.search(filters, page = 1)) {
                is Result.Success -> {
                    val related = result.data.data
                        .filter { it.id != wallpaperId }
                        .take(20)
                    _uiState.update {
                        it.copy(related = related, relatedLoading = false)
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(relatedLoading = false) }
                }
            }
        }
    }
}
