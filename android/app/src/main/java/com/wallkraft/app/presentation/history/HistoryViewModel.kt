package com.wallkraft.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallkraft.app.data.db.WallpaperHistoryDao
import com.wallkraft.app.data.db.WallpaperHistoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val wallpaperHistoryDao: WallpaperHistoryDao,
) : ViewModel() {

    val history: StateFlow<List<WallpaperHistoryEntity>> =
        wallpaperHistoryDao.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteEntry(entity: WallpaperHistoryEntity) {
        viewModelScope.launch {
            wallpaperHistoryDao.deleteById(entity.wallpaperId, entity.setAt)
        }
    }
}
