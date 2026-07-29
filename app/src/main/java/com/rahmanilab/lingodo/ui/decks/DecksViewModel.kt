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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DecksViewModel(
    private val deckRepository: DeckRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val decks: StateFlow<List<DeckStats>> =
        settings.activePairId.flatMapLatest { pairId -> deckRepository.observeDeckStats(pairId) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createDeck(name: String, description: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            deckRepository.createDeck(name, description, settings.currentActivePairId())
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

    fun deleteDeck(id: Long) {
        viewModelScope.launch {
            deckRepository.getDeck(id)?.let { deckRepository.deleteDeck(it) }
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
