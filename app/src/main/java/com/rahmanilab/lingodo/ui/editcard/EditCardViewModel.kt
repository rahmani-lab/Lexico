package com.rahmanilab.lingodo.ui.editcard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rahmanilab.lingodo.data.local.entity.CardEntity
import com.rahmanilab.lingodo.data.local.entity.DeckEntity
import com.rahmanilab.lingodo.data.preferences.SettingsRepository
import com.rahmanilab.lingodo.data.repository.CardRepository
import com.rahmanilab.lingodo.data.repository.DeckRepository
import com.rahmanilab.lingodo.data.repository.LanguagePairRepository
import com.rahmanilab.lingodo.domain.autofill.AutoFillData
import com.rahmanilab.lingodo.domain.autofill.AutoFillEngine
import com.rahmanilab.lingodo.domain.autofill.AutoFillOutcome
import com.rahmanilab.lingodo.domain.model.Example
import com.rahmanilab.lingodo.domain.model.WordForm
import com.rahmanilab.lingodo.ui.appContainer
import androidx.lifecycle.createSavedStateHandle
import com.rahmanilab.lingodo.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    val meaning: String = "",
    val englishDefinition: String = "",
    val examples: List<Example> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val collocations: List<String> = emptyList(),
    val wordForms: List<WordForm> = emptyList(),
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
    val saved: Boolean = false,
    val autoFilling: Boolean = false
) {
    val canSave: Boolean
        get() = form.word.isNotBlank() && form.meaning.isNotBlank()
}

class EditCardViewModel(
    private val cardRepository: CardRepository,
    private val deckRepository: DeckRepository,
    private val languagePairRepository: LanguagePairRepository,
    private val autoFillEngine: AutoFillEngine,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var activePairId: Long = DeckEntity.DEFAULT_PAIR_ID
    private var activeSourceCode: String = "fa"
    private var activeTargetCode: String = "en"

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val cardId: Long = savedStateHandle[Routes.ARG_CARD_ID] ?: Routes.NO_ID
    private val initialDeckId: Long = savedStateHandle[Routes.ARG_DECK_ID] ?: Routes.NO_ID

    private val _uiState = MutableStateFlow(EditCardUiState())
    val uiState: StateFlow<EditCardUiState> = _uiState.asStateFlow()

    private var originalWord: String = ""
    private var originalDeckId: Long = Routes.NO_ID
    private var originalCreatedAt: Long = 0L

    init {
        viewModelScope.launch {
            // Decks/tags for the active language pair, for the selector and suggestions.
            val activePair = languagePairRepository.activePair()
            activePairId = activePair.id
            activeSourceCode = activePair.source.code
            activeTargetCode = activePair.target.code
            val decks = deckRepository.observeDecksForPair(activePairId).first()
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
                                meaning = c.meaning,
                                englishDefinition = c.englishDefinition,
                                examples = c.examples,
                                synonyms = c.synonyms,
                                antonyms = c.antonyms,
                                collocations = c.collocations,
                                wordForms = c.wordForms,
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

            // Creating a new card: prefer the deck the screen was opened for, then the deck the user
            // last added to (so adding a new deck elsewhere doesn't hijack the selection), then any
            // deck. The TTS locale defaults to the active pair's target language.
            val lastUsedDeckId = settingsRepository.currentLastDeckId()
            val defaultDeckId = decks.firstOrNull { it.id == initialDeckId }?.id
                ?: decks.firstOrNull { it.id == lastUsedDeckId }?.id
                ?: decks.firstOrNull()?.id
                ?: Routes.NO_ID
            _uiState.update {
                it.copy(
                    form = it.form.copy(deckId = defaultDeckId, languageCode = activePair.target.ttsTag),
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
    fun setMeaning(v: String) = edit { it.copy(meaning = v) }
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

    fun addWordForm() = edit { it.copy(wordForms = it.wordForms + WordForm("", "")) }
    fun removeWordForm(index: Int) = edit {
        it.copy(wordForms = it.wordForms.filterIndexed { i, _ -> i != index })
    }
    fun setWordFormLabel(index: Int, label: String) = edit {
        it.copy(wordForms = it.wordForms.mapIndexed { i, w -> if (i == index) w.copy(label = label) else w })
    }
    fun setWordFormValue(index: Int, form: String) = edit {
        it.copy(wordForms = it.wordForms.mapIndexed { i, w -> if (i == index) w.copy(form = form) else w })
    }

    fun addTag(v: String) = edit { it.copy(tags = it.tags.addToken(v)) }
    fun removeTag(v: String) = edit { it.copy(tags = it.tags - v) }

    private fun List<String>.addToken(value: String): List<String> {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || any { it.equals(trimmed, ignoreCase = true) }) return this
        return this + trimmed
    }

    /**
     * User-triggered (✨) auto-fill. Enriches the card from the word and active pair, merging results
     * into empty fields only — never overwriting anything the user typed.
     */
    fun autoFill() {
        val form = _uiState.value.form
        val word = form.word.trim()
        if (word.isBlank()) {
            _messages.tryEmit("Type a word first, then tap auto-fill.")
            return
        }
        // A part of speech the user already chose pins the sense the engine must describe.
        val pinnedPos = form.partOfSpeech.trim()
        viewModelScope.launch {
            _uiState.update { it.copy(autoFilling = true) }
            when (val outcome = autoFillEngine.enrich(word, activeSourceCode, activeTargetCode, pinnedPos)) {
                is AutoFillOutcome.Success -> {
                    mergeIntoEmptyFields(outcome.data)
                    _messages.tryEmit(
                        if (pinnedPos.isBlank()) "Auto-filled the empty fields."
                        else "Auto-filled the empty fields for \"$pinnedPos\"."
                    )
                }
                is AutoFillOutcome.Unavailable -> _messages.tryEmit(outcome.reason)
                is AutoFillOutcome.Error -> _messages.tryEmit(outcome.message)
            }
            _uiState.update { it.copy(autoFilling = false) }
        }
    }

    private fun mergeIntoEmptyFields(data: AutoFillData) = edit { f ->
        f.copy(
            phonetic = f.phonetic.ifBlank { data.phonetic },
            partOfSpeech = f.partOfSpeech.ifBlank { data.partOfSpeech },
            meaning = f.meaning.ifBlank { data.meaning },
            englishDefinition = f.englishDefinition.ifBlank { data.definition },
            examples = f.examples.ifEmpty { data.examples },
            synonyms = f.synonyms.ifEmpty { data.synonyms },
            antonyms = f.antonyms.ifEmpty { data.antonyms },
            collocations = f.collocations.ifEmpty { data.collocations },
            wordForms = f.wordForms.ifEmpty { data.wordForms },
            tags = f.tags.ifEmpty { data.tags }
        )
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
                meaning = form.meaning.trim(),
                englishDefinition = form.englishDefinition.trim(),
                examples = form.examples.filter { it.text.isNotBlank() },
                synonyms = form.synonyms,
                antonyms = form.antonyms,
                collocations = form.collocations,
                wordForms = form.wordForms.filter { it.form.isNotBlank() },
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
            // Remember the deck so the next "Add card" defaults to it.
            settingsRepository.setLastDeckId(deckId)
            _uiState.update { it.copy(saved = true) }
        }
    }

    /** Guarantee a deck exists to attach the card to, creating a default one if necessary. */
    private suspend fun ensureDeck(deckId: Long): Long {
        if (deckId > 0) return deckId
        return deckRepository.createDeck("My Words", "", activePairId)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                EditCardViewModel(
                    appContainer.cardRepository,
                    appContainer.deckRepository,
                    appContainer.languagePairRepository,
                    appContainer.autoFillEngine,
                    appContainer.settingsRepository,
                    createSavedStateHandle()
                )
            }
        }
    }
}
