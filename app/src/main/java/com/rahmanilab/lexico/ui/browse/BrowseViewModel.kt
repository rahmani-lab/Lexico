package com.rahmanilab.lexico.ui.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rahmanilab.lexico.data.local.entity.DeckEntity
import com.rahmanilab.lexico.data.local.relation.CardWithDetails
import com.rahmanilab.lexico.data.repository.CardRepository
import com.rahmanilab.lexico.data.repository.DeckRepository
import com.rahmanilab.lexico.ui.appContainer
import com.rahmanilab.lexico.ui.navigation.Routes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BrowseFilter(val label: String) {
    ALL("All"),
    NEW("New"),
    LEARNING("Learning"),
    DUE("Due"),
    DIFFICULT("Difficult")
}

data class BrowseUiState(
    val cards: List<CardWithDetails> = emptyList(),
    val decks: List<DeckEntity> = emptyList(),
    val tags: List<String> = emptyList(),
    val query: String = "",
    val deckFilter: Long? = null,
    val tagFilter: String? = null,
    val stateFilter: BrowseFilter = BrowseFilter.ALL,
    val loading: Boolean = true
)

class BrowseViewModel(
    private val cardRepository: CardRepository,
    private val deckRepository: DeckRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val deckFilter = MutableStateFlow(savedStateHandle.get<Long>(Routes.ARG_DECK_ID)?.takeIf { it > 0 })
    private val tagFilter = MutableStateFlow<String?>(null)
    private val stateFilter = MutableStateFlow(BrowseFilter.ALL)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val sourceCards = query
        .debounce(200)
        .flatMapLatest { q ->
            if (q.isBlank()) cardRepository.observeAll() else cardRepository.search(q)
        }

    private val filteredCards = combine(
        sourceCards, deckFilter, tagFilter, stateFilter
    ) { cards, deck, tag, filter ->
        val now = System.currentTimeMillis()
        cards.filter { card ->
            (deck == null || card.card.deckId == deck) &&
                (tag == null || card.tags.any { it.name.equals(tag, ignoreCase = true) }) &&
                matchesState(card, filter, now)
        }
    }

    val uiState: StateFlow<BrowseUiState> = combine(
        filteredCards,
        deckRepository.observeDecks(),
        cardRepository.observeTags()
    ) { cards, decks, tags ->
        BrowseUiState(
            cards = cards,
            decks = decks,
            tags = tags.map { it.name },
            query = query.value,
            deckFilter = deckFilter.value,
            tagFilter = tagFilter.value,
            stateFilter = stateFilter.value,
            loading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowseUiState())

    private fun matchesState(card: CardWithDetails, filter: BrowseFilter, now: Long): Boolean {
        val schedule = card.schedule
        return when (filter) {
            BrowseFilter.ALL -> true
            BrowseFilter.NEW -> schedule?.state == "NEW"
            BrowseFilter.LEARNING -> schedule?.state == "LEARNING" || schedule?.state == "RELEARNING"
            BrowseFilter.DUE -> schedule != null && schedule.state != "NEW" && schedule.dueAt <= now
            BrowseFilter.DIFFICULT -> (schedule?.lapses ?: 0) >= 2
        }
    }

    fun setQuery(value: String) { query.value = value }
    fun setDeckFilter(value: Long?) { deckFilter.value = value }
    fun setTagFilter(value: String?) { tagFilter.value = value }
    fun setStateFilter(value: BrowseFilter) { stateFilter.value = value }

    fun deleteCard(card: CardWithDetails) {
        viewModelScope.launch { cardRepository.deleteCard(card.card) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                BrowseViewModel(
                    appContainer.cardRepository,
                    appContainer.deckRepository,
                    createSavedStateHandle()
                )
            }
        }
    }
}
