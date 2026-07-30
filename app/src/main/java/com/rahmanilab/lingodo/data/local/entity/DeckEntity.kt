package com.rahmanilab.lingodo.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A collection ("Deck") that groups related cards, e.g. "IELTS Vocabulary". Each deck belongs to a
 * language pair ([languagePairId]) so decks and their cards can be filtered by the active workspace.
 *
 * Decks form a shallow two-level hierarchy: a top-level "book" has [parentId] == null, and a
 * "lesson" sets [parentId] to its book. A book may also hold its own cards; studying a book reviews
 * its own cards plus all of its lessons' cards.
 */
@Serializable
@Entity(tableName = "decks", indices = [Index("languagePairId"), Index("parentId")])
data class DeckEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long,
    val languagePairId: Long = DEFAULT_PAIR_ID,
    /** The parent "book" this deck is a lesson of, or null for a top-level deck. */
    val parentId: Long? = null
) {
    companion object {
        const val DEFAULT_PAIR_ID = 1L
    }
}
