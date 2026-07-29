package com.rahmanilab.lingodo.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A collection ("Deck") that groups related cards, e.g. "IELTS Vocabulary". Each deck belongs to a
 * language pair ([languagePairId]) so decks and their cards can be filtered by the active workspace.
 */
@Serializable
@Entity(tableName = "decks", indices = [Index("languagePairId")])
data class DeckEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long,
    val languagePairId: Long = DEFAULT_PAIR_ID
) {
    companion object {
        const val DEFAULT_PAIR_ID = 1L
    }
}
