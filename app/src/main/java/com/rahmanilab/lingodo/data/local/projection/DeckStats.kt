package com.rahmanilab.lingodo.data.local.projection

/**
 * Aggregated counts for a deck, produced by a single grouped query so the Decks screen can render
 * progress without loading every card.
 */
data class DeckStats(
    val id: Long,
    val name: String,
    val description: String,
    val createdAt: Long,
    val languagePairId: Long,
    val total: Int,
    val newCount: Int,
    val dueCount: Int,
    val learnedCount: Int
)
