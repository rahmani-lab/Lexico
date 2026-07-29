package com.rahmanilab.lingodo.domain.autofill

import com.rahmanilab.lingodo.domain.model.Example
import com.rahmanilab.lingodo.domain.model.WordForm

/**
 * Everything the auto-fill engine can discover about a word. All fields are optional; the engine
 * fills what it can and the caller merges the result into empty card fields only.
 */
data class AutoFillData(
    val phonetic: String = "",
    val partOfSpeech: String = "",
    /** Short meaning/translation in the source (native) language. */
    val meaning: String = "",
    val definition: String = "",
    val examples: List<Example> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val collocations: List<String> = emptyList(),
    val wordForms: List<WordForm> = emptyList(),
    val audioUrl: String? = null
)

/** Result of an auto-fill attempt. */
sealed interface AutoFillOutcome {
    data class Success(val data: AutoFillData) : AutoFillOutcome
    /** The engine ran but has nothing to offer yet (e.g. no provider configured). */
    data class Unavailable(val reason: String) : AutoFillOutcome
    data class Error(val message: String) : AutoFillOutcome
}

/**
 * Enriches a card from just the target word and the active language pair.
 *
 * The intended production design is a 3-tier fallback:
 *  1. A keyless dictionary (phonetics, part of speech, audio).
 *  2. A free AI tier (contextual meanings, examples, collocations).
 *  3. A BYOK LLM for deep, JSON-structured metadata — including auto-detecting the word's
 *     grammatical class and extracting its word family and inflections (verb tenses, comparatives…).
 *
 * The engine is always **user-triggered** (never on keystroke) and its result is merged
 * non-destructively into empty fields only.
 */
interface AutoFillEngine {
    suspend fun enrich(word: String, sourceCode: String, targetCode: String): AutoFillOutcome
}
