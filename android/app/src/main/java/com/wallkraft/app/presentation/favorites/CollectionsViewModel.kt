package com.wallkraft.app.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallkraft.app.domain.model.Collection
import com.wallkraft.app.domain.repository.CollectionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Result of [quickAdd] — tells the caller what happened.
 */
sealed class QuickAddResult {
    /** 0 collections: auto-created "Saved" and added there. Caller shows snackbar with undo. */
    data class AutoCreated(val collectionId: Long, val collectionName: String) : QuickAddResult()
    /** 1 collection: one-tap added. Caller shows brief snackbar confirmation. */
    data class OneTapAdded(val collectionId: Long, val collectionName: String) : QuickAddResult()
    /** N≥2 collections: caller should show the existing AddToCollectionDialog. */
    data class ShowDialog(val ids: Set<String>) : QuickAddResult()
}

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val collectionsRepository: CollectionsRepository,
) : ViewModel() {

    val collections: StateFlow<List<Collection>> = collectionsRepository.observeAll()
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
        viewModelScope.launch {
            try {
                collectionsRepository.delete(id)
            } catch (e: Exception) {
                if (com.wallkraft.app.BuildConfig.DEBUG) {
                    android.util.Log.e("CollectionsViewModel", "Failed to delete collection $id", e)
                }
            }
        }
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

    /**
     * Smart add: encapsulates the 0/1/N collection decision.
     *
     * - **0 collections** → auto-creates "Saved" and adds [ids] there.
     *   Returns [QuickAddResult.AutoCreated] so the caller can show an undo
     *   snackbar (undo = removeItems).
     * - **1 collection** → one-tap add. Returns [QuickAddResult.OneTapAdded].
     * - **N≥2 collections** → returns [QuickAddResult.ShowDialog] with the
     *   wallpaper ids so the caller opens the existing [AddToCollectionDialog].
     */
    fun quickAdd(ids: Set<String>, onResult: (QuickAddResult) -> Unit) {
        viewModelScope.launch {
            val current = collections.value
            when {
                current.isEmpty() -> {
                    val colId = collectionsRepository.create("Saved")
                    if (colId > 0) ids.forEach { collectionsRepository.addTo(colId, it) }
                    onResult(QuickAddResult.AutoCreated(colId, "Saved"))
                }
                current.size == 1 -> {
                    val col = current.first()
                    ids.forEach { collectionsRepository.addTo(col.id, it) }
                    onResult(QuickAddResult.OneTapAdded(col.id, col.name))
                }
                else -> onResult(QuickAddResult.ShowDialog(ids))
            }
        }
    }

    /**
     * Undo for [QuickAddResult.AutoCreated]: removes the auto-created items
     * so the snackbar undo works. Call [delete] to fully remove the collection.
     */
    fun removeItems(collectionId: Long, wallpaperIds: List<String>) {
        viewModelScope.launch {
            wallpaperIds.forEach { collectionsRepository.removeFrom(collectionId, it) }
        }
    }
}
