package com.rahmanilab.lingodo.data.repository

import androidx.room.withTransaction
import com.rahmanilab.lingodo.data.local.LingoDoDatabase
import com.rahmanilab.lingodo.data.local.entity.CardEntity
import com.rahmanilab.lingodo.data.local.entity.CardLinkCrossRef
import com.rahmanilab.lingodo.data.local.entity.CardTagCrossRef
import com.rahmanilab.lingodo.data.local.entity.TagEntity
import com.rahmanilab.lingodo.data.local.relation.CardWithDetails
import kotlinx.coroutines.flow.Flow

class CardRepository(private val db: LingoDoDatabase) {

    private val cardDao get() = db.cardDao()
    private val scheduleDao get() = db.cardScheduleDao()
    private val tagDao get() = db.tagDao()

    // --- reads ---

    fun observeCard(id: Long): Flow<CardWithDetails?> = cardDao.observeCardWithDetails(id)

    suspend fun getCard(id: Long): CardWithDetails? = cardDao.getCardWithDetails(id)

    fun observeAll(): Flow<List<CardWithDetails>> = cardDao.observeAllWithDetails()

    fun observeForDeck(deckId: Long): Flow<List<CardWithDetails>> = cardDao.observeCardsForDeck(deckId)

    fun search(query: String): Flow<List<CardWithDetails>> = cardDao.searchWithDetails(query.trim())

    fun observeTags(): Flow<List<TagEntity>> = tagDao.observeAll()

    // --- duplicate detection ---

    suspend fun countWithWordInDeck(deckId: Long, word: String): Int =
        cardDao.countByWordInDeck(deckId, word.trim())

    suspend fun countWithWord(word: String): Int = cardDao.countByWord(word.trim())

    // --- writes ---

    /**
     * Insert a new card, give it an initial (NEW, due-now) schedule and attach its tags — all in a
     * single transaction so a card can never exist without a schedule.
     */
    suspend fun addCard(card: CardEntity, tags: List<String>): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val id = cardDao.insert(card.copy(id = 0, createdAt = now, updatedAt = now))
        scheduleDao.upsert(initialSchedule(id, now))
        applyTags(id, tags)
        id
    }

    suspend fun updateCard(card: CardEntity, tags: List<String>) = db.withTransaction {
        cardDao.update(card.copy(updatedAt = System.currentTimeMillis()))
        applyTags(card.id, tags)
        tagDao.deleteOrphanTags()
    }

    suspend fun deleteCard(card: CardEntity) = db.withTransaction {
        cardDao.delete(card)
        tagDao.deleteOrphanTags()
    }

    /** Every card in a language pair, for pickers such as the card-link chooser. */
    suspend fun getCardsForPair(pairId: Long): List<CardEntity> = cardDao.getCardsForPair(pairId)

    // --- Concept clusters: links between related / easily confused cards ---

    private val cardLinkDao get() = db.cardLinkDao()

    /** Ids of the cards linked to [cardId]. */
    suspend fun linkedCardIds(cardId: Long): List<Long> = cardLinkDao.linkedIds(cardId)

    /** The cards linked to [cardId], for showing the cluster on the edit screen. */
    suspend fun linkedCards(cardId: Long): List<CardEntity> = cardLinkDao.linkedCards(cardId)

    /**
     * Replace [cardId]'s cluster with [linkedIds]. Links are stored in both directions so either
     * card finds the other; self-links are ignored.
     */
    suspend fun setLinkedCards(cardId: Long, linkedIds: List<Long>) = db.withTransaction {
        cardLinkDao.deleteAllForCard(cardId)
        val targets = linkedIds.filter { it > 0 && it != cardId }.distinct()
        if (targets.isNotEmpty()) {
            cardLinkDao.insertAll(
                targets.flatMap {
                    listOf(CardLinkCrossRef(cardId, it), CardLinkCrossRef(it, cardId))
                }
            )
        }
    }

    suspend fun deleteCardById(id: Long) = db.withTransaction {
        cardDao.deleteById(id)
        tagDao.deleteOrphanTags()
    }

    /** Replaces the card's tag set, creating any tags that don't exist yet. */
    private suspend fun applyTags(cardId: Long, tags: List<String>) {
        tagDao.clearTagsForCard(cardId)
        tags.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .forEach { name ->
                val tagId = tagDao.getByName(name)?.id
                    ?: tagDao.insertTag(TagEntity(name = name)).takeIf { it != -1L }
                    ?: tagDao.getByName(name)?.id
                    ?: return@forEach
                tagDao.insertCrossRef(CardTagCrossRef(cardId = cardId, tagId = tagId))
            }
    }
}
