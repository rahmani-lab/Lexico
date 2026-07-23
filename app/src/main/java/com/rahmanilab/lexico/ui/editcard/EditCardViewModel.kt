package com.rahmanilab.lexico.ui.editcard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rahmanilab.lexico.data.local.entity.CardEntity
import com.rahmanilab.lexico.data.local.entity.DeckEntity
import com.rahmanilab.lexico.data.repository.CardRepository
import com.rahmanilab.lexico.data.repository.DeckRepository
import com.rahmanilab.lexico.domain.model.Example
import com.rahmanilab.lexico.ui.appContainer
import androidx.lifecycle.createSavedStateHandle
import com.rahmanilab.lexico.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Editable form model for a card. */
data class EditCardForm(
    val deckId: Long = Routes.NO_ID,
    val word: String = "",
    val partOfSpeech: String = "",
    val phonetic: String = "",
    val pronunciationHint: String = "",
    val persianMeaning: String = "",
    val englishDefinition: String = "",
    val examples: List<Example> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val collocations: List<String> = emptyList(),
    val notes: String = "",
    val source: String = "",
    val imageUri: String? = null,
    val languageCode: String = "en-US",
    val tags: List<String> = emptyList()
)

data class EditCardUiState(
    val form: EditCardForm = EditCardForm(),
    val isEditing: Boolean = false,
    val decks: List<DeckEntity> = emptyList(),
    val tagSuggestions: List<String> = emptyList(),
    val duplicateWarning: Boolean = false,
    val loading: Boolean = true,
    val saved: Boolean = false
) {
    val canSave: Boolean
        get() = form.word.isNotBlank() && form.persianMeaning.isNotBlank()
}

class EditCardViewModel(
    private val cardRepository: CardRepository,
    private val deckRepository: DeckRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardId: Long = savedStateHandle[Routes.ARG_CARD_ID] ?: Routes.NO_ID
    private val initialDeckId: Long = savedStateHandle[Routes.ARG_DECK_ID] ?: Routes.NO_ID

    private val _uiState = MutableStateFlow(EditCardUiState())
    val uiState: StateFlow<EditCardUiState> = _uiState.asStateFlow()

    private var originalWord: String = ""
    private var originalDeckId: Long = Routes.NO_ID
    private var originalCreatedAt: Long = 0L

    init {
        viewModelScope.launch {
            // one-shot read of the current decks and tags for the selector / suggestions
            val decks = deckRepository.observeDecks().first()
            val tags = cardRepository.observeTags().first().map { it.name }

            if (cardId != Routes.NO_ID) {
                val details = cardRepository.getCard(cardId)
                if (details != null) {
                    val c = details.card
                    originalWord = c.word
                    originalDeckId = c.deckId
                    originalCreatedAt = c.createdAt
                    _uiState.update {
                        it.copy(
                            form = EditCardForm(
                                deckId = c.deckId,
                                word = c.word,
                                partOfSpeech = c.partOfSpeech,
                                phonetic = c.phonetic,
                                pronunciationHint = c.pronunciationHint,
                                persianMeaning = c.persianMeaning,
                                englishDefinition = c.englishDefinition,
                                examples = c.examples,
                                synonyms = c.synonyms,
                                antonyms = c.antonyms,
                                collocations = c.collocations,
                                notes = c.notes,
                                source = c.source,
                                imageUri = c.imageUri,
                                languageCode = c.languageCode,
                                tags = details.tags.map { t -> t.name }
                            ),
                            isEditing = true,
                            decks = decks,
                            tagSuggestions = tags,
                            loading = false
                        )
                    }
                    return@launch
                }
            }

            // Creating a new card: choose a sensible default deck.
            val defaultDeckId = decks.firstOrNull { it.id == initialDeckId }?.id
                ?: decks.firstOrNull()?.id
                ?: Routes.NO_ID
            _uiState.update {
                it.copy(
                    form = it.form.copy(deckId = defaultDeckId),
                    isEditing = false,
                    decks = decks,
                    tagSuggestions = tags,
                    loading = false
                )
            }
        }
    }

    private fun edit(block: (EditCardForm) -> EditCardForm) {
        _uiState.update { it.copy(form = block(it.form)) }
    }

    fun setDeck(id: Long) { edit { it.copy(deckId = id) }; refreshDuplicate() }
    fun setWord(v: String) { edit { it.copy(word = v) }; refreshDuplicate() }
    fun setPartOfSpeech(v: String) = edit { it.copy(partOfSpeech = v) }
    fun setPhonetic(v: String) = edit { it.copy(phonetic = v) }
    fun setPronunciationHint(v: String) = edit { it.copy(pronunciationHint = v) }
    fun setPersianMeaning(v: String) = edit { it.copy(persianMeaning = v) }
    fun setEnglishDefinition(v: String) = edit { it.copy(englishDefinition = v) }
    fun setNotes(v: String) = edit { it.copy(notes = v) }
    fun setSource(v: String) = edit { it.copy(source = v) }
    fun setImageUri(v: String?) = edit { it.copy(imageUri = v) }
    fun setLanguageCode(v: String) = edit { it.copy(languageCode = v) }

    fun addExample() = edit { it.copy(examples = it.examples + Example("", "")) }
    fun removeExample(index: Int) = edit {
        it.copy(examples = it.examples.filterIndexed { i, _ -> i != index })
    }
    fun setExampleText(index: Int, text: String) = edit {
        it.copy(examples = it.examples.mapIndexed { i, e -> if (i == index) e.copy(text = text) else e })
    }
    fun setExampleTranslation(index: Int, text: String) = edit {
        it.copy(examples = it.examples.mapIndexed { i, e -> if (i == index) e.copy(translation = text) else e })
    }

    fun addSynonym(v: String) = edit { it.copy(synonyms = it.synonyms.addToken(v)) }
    fun removeSynonym(v: String) = edit { it.copy(synonyms = it.synonyms - v) }
    fun addAntonym(v: String) = edit { it.copy(antonyms = it.antonyms.addToken(v)) }
    fun removeAntonym(v: String) = edit { it.copy(antonyms = it.antonyms - v) }
    fun addCollocation(v: String) = edit { it.copy(collocations = it.collocations.addToken(v)) }
    fun removeCollocation(v: String) = edit { it.copy(collocations = it.collocations - v) }
    fun addTag(v: String) = edit { it.copy(tags = it.tags.addToken(v)) }
    fun removeTag(v: String) = edit { it.copy(tags = it.tags - v) }

    private fun List<String>.addToken(value: String): List<String> {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || any { it.equals(trimmed, ignoreCase = true) }) return this
        return this + trimmed
    }

    private fun refreshDuplicate() {
        val form = _uiState.value.form
        if (form.word.isBlank() || form.deckId <= 0) {
            _uiState.update { it.copy(duplicateWarning = false) }
            return
        }
        viewModelScope.launch {
            val count = cardRepository.countWithWordInDeck(form.deckId, form.word)
            val self = if (_uiState.value.isEditing &&
                originalWord.equals(form.word.trim(), ignoreCase = true) &&
                originalDeckId == form.deckId
            ) 1 else 0
            _uiState.update { it.copy(duplicateWarning = (count - self) > 0) }
        }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            val form = state.form
            val deckId = ensureDeck(form.deckId)
            val entity = CardEntity(
                id = if (state.isEditing) cardId else 0,
                deckId = deckId,
                word = form.word.trim(),
                partOfSpeech = form.partOfSpeech.trim(),
                phonetic = form.phonetic.trim(),
                pronunciationHint = form.pronunciationHint.trim(),
                persianMeaning = form.persianMeaning.trim(),
                englishDefinition = form.englishDefinition.trim(),
                examples = form.examples.filter { it.text.isNotBlank() },
                synonyms = form.synonyms,
                antonyms = form.antonyms,
                collocations = form.collocations,
                notes = form.notes.trim(),
                source = form.source.trim(),
                imageUri = form.imageUri,
                languageCode = form.languageCode,
                // Preserved on edit; overwritten by addCard() for new cards.
                createdAt = if (state.isEditing) originalCreatedAt else 0L,
                updatedAt = 0L
            )
            if (state.isEditing) {
                cardRepository.updateCard(entity, form.tags)
            } else {
                cardRepository.addCard(entity, form.tags)
            }
            _uiState.update { it.copy(saved = true) }
        }
    }

    /** Guarantee a deck exists to attach the card to, creating a default one if necessary. */
    private suspend fun ensureDeck(deckId: Long): Long {
        if (deckId > 0) return deckId
        return deckRepository.createDeck("My Words", "")
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                EditCardViewModel(
                    appContainer.cardRepository,
                    appContainer.deckRepository,
                    createSavedStateHandle()
                )
            }
        }
    }
}
