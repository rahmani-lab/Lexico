package com.rahmanilab.lingodo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rahmanilab.lingodo.domain.model.Example
import com.rahmanilab.lingodo.domain.model.WordForm
import kotlinx.serialization.Serializable

/**
 * A single flashcard.
 *
 * The card supports full words as well as multi-word phrases and phrasal verbs (there is no
 * assumption anywhere that [word] is a single token). Structured lists ([examples], [synonyms],
 * [antonyms], [collocations]) are persisted as JSON through the Room type converters so that a
 * card can hold *several* examples and meanings.
 */
@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("deckId"), Index("word")]
)
@Serializable
data class CardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val deckId: Long,

    // --- Front of the card ---
    val word: String,
    val partOfSpeech: String = "",
    val phonetic: String = "",
    /** Short, human friendly pronunciation hint, e.g. "ri-ZIL-ee-uhnt". */
    val pronunciationHint: String = "",

    // --- Back of the card ---
    val meaning: String,
    val englishDefinition: String = "",
    val examples: List<Example> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val collocations: List<String> = emptyList(),
    /** Grammatical forms, inflections and word-family derivatives (auto-detectable). */
    val wordForms: List<WordForm> = emptyList(),
    val notes: String = "",
    /** Where the learner first met the word (a book, a movie, a conversation…). */
    val source: String = "",
    val imageUri: String? = null,

    /** Per-card TTS locale/accent, e.g. "en-US" or "en-GB". */
    val languageCode: String = "en-US",

    val createdAt: Long,
    val updatedAt: Long
)
