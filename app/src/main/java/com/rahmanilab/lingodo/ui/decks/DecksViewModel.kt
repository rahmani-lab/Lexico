package com.rahmanilab.lingodo.ui.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rahmanilab.lingodo.data.local.projection.DeckStats
import com.rahmanilab.lingodo.data.preferences.SettingsRepository
import com.rahmanilab.lingodo.data.repository.DeckRepository
import com.rahmanilab.lingodo.ui.appContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A top-level deck ("book") together with its lessons (sub-decks). Counts are aggregated so the
 * Decks screen can show a book's combined workload at a glance.
 */
data class DeckGroup(
    val parent: DeckStats,
    val children: List<DeckStats>
) {
    val hasChildren: Boolean get() = children.isNotEmpty()
    val totalCards: Int get() = parent.total + children.sumOf { it.total }
    val dueCount: Int get() = parent.dueCount + children.sumOf { it.dueCount }
    val newCount: Int get() = parent.newCount + children.sumOf { it.newCount }
    val learnedCount: Int get() = parent.learnedCount + children.sumOf { it.learnedCount }
}

class DecksViewModel(
    private val deckRepository: DeckRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val groups: StateFlow<List<DeckGroup>> =
        settings.activePairId.flatMapLatest { pairId -> deckRepository.observeDeckStats(pairId) }
            .map { flat -> buildGroups(flat) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun buildGroups(flat: List<DeckStats>): List<DeckGroup> {
        val childrenByParent = flat.filter { it.parentId != null }.groupBy { it.parentId }
        return flat.filter { it.parentId == null }
            .map { top -> DeckGroup(top, childrenByParent[top.id].orEmpty()) }
    }

    /** Create a top-level deck, or a lesson when [parentId] is given (it inherits the active pair). */
    fun createDeck(name: String, description: String, parentId: Long? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            deckRepository.createDeck(name, description, settings.currentActivePairId(), parentId)
        }
    }

    fun updateDeck(id: Long, name: String, description: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            deckRepository.getDeck(id)?.let {
                deckRepository.updateDeck(it.copy(name = name.trim(), description = description.trim()))
            }
        }
    }

    /** Delete a single deck (a lesson, or a book with no lessons); its cards cascade. */
    fun deleteDeck(id: Long) {
        viewModelScope.launch {
            deckRepository.getDeck(id)?.let { deckRepository.deleteDeck(it) }
        }
    }

    /** Delete a book and all of its lessons (and their cards). */
    fun deleteBookAndLessons(id: Long) {
        viewModelScope.launch {
            deckRepository.getDeck(id)?.let { deckRepository.deleteDeckAndChildren(it) }
        }
    }

    /** Delete a book but keep its lessons, promoting them to top-level decks. */
    fun deleteBookKeepLessons(id: Long) {
        viewModelScope.launch {
            deckRepository.getDeck(id)?.let { deckRepository.deleteDeckPromotingChildren(it) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                DecksViewModel(appContainer.deckRepository, appContainer.settingsRepository)
            }
        }
    }
}
