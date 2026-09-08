package com.wallkraft.app.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallkraft.app.data.db.CollectionWithItems
import com.wallkraft.app.domain.repository.CollectionsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CollectionsViewModel(
    private val collectionsRepository: CollectionsRepository,
) : ViewModel() {

    val collections: StateFlow<List<CollectionWithItems>> = collectionsRepository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun create(name: String, onResult: (Long) -> Unit = {}) {
        viewModelScope.launch { onResult(collectionsRepository.create(name)) }
    }

    /**
     * Renames, reporting success. False means the name is taken by another
     * collection — the caller keeps the dialog open and says so. Results
     * ride the same onResult callback pattern as [create]: no event flows,
     * no timing edges.
     */
    fun rename(id: Long, name: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch { onResult(collectionsRepository.rename(id, name)) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { collectionsRepository.delete(id) }
    }

    /** Undo for delete: recreates the collection and re-adds surviving members. */
    fun restore(name: String, wallpaperIds: List<String>) {
        viewModelScope.launch {
            val id = collectionsRepository.create(name)
            if (id > 0) wallpaperIds.forEach { collectionsRepository.addTo(id, it) }
        }
    }

    fun setMember(collectionId: Long, wallpaperId: String, member: Boolean) {
        viewModelScope.launch {
            if (member) {
                collectionsRepository.addTo(collectionId, wallpaperId)
            } else {
                collectionsRepository.removeFrom(collectionId, wallpaperId)
            }
        }
    }
}
