package com.rahmanilab.lingodo.data.repository

import com.rahmanilab.lingodo.data.local.dao.DeckDao
import com.rahmanilab.lingodo.data.local.entity.DeckEntity
import com.rahmanilab.lingodo.data.local.projection.DeckStats
import kotlinx.coroutines.flow.Flow

class DeckRepository(private val deckDao: DeckDao) {

    fun observeDecks(): Flow<List<DeckEntity>> = deckDao.observeAll()

    fun observeDecksForPair(pairId: Long): Flow<List<DeckEntity>> = deckDao.observeByPair(pairId)

    fun observeDeckStats(pairId: Long, now: Long = System.currentTimeMillis()): Flow<List<DeckStats>> =
        deckDao.observeDeckStats(now, pairId)

    fun observeDeck(id: Long): Flow<DeckEntity?> = deckDao.observeById(id)

    suspend fun getDeck(id: Long): DeckEntity? = deckDao.getById(id)

    suspend fun deckCount(): Int = deckDao.count()

    /** A parent deck's direct lessons (sub-decks). */
    suspend fun getChildren(parentId: Long): List<DeckEntity> = deckDao.getChildren(parentId)

    suspend fun createDeck(
        name: String,
        description: String = "",
        languagePairId: Long = DeckEntity.DEFAULT_PAIR_ID,
        parentId: Long? = null
    ): Long =
        deckDao.insert(
            DeckEntity(
                name = name.trim(),
                description = description.trim(),
                createdAt = System.currentTimeMillis(),
                languagePairId = languagePairId,
                parentId = parentId
            )
        )

    suspend fun updateDeck(deck: DeckEntity) = deckDao.update(deck)

    /** Delete a single deck; its cards cascade via the foreign key. */
    suspend fun deleteDeck(deck: DeckEntity) = deckDao.delete(deck)

    /** Delete a book together with all its lessons (and, by cascade, every card in them). */
    suspend fun deleteDeckAndChildren(deck: DeckEntity) {
        deckDao.getChildren(deck.id).forEach { deckDao.delete(it) }
        deckDao.delete(deck)
    }

    /** Delete a book but keep its lessons, promoting them to top-level decks. */
    suspend fun deleteDeckPromotingChildren(deck: DeckEntity) {
        deckDao.getChildren(deck.id).forEach { deckDao.update(it.copy(parentId = null)) }
        deckDao.delete(deck)
    }
}
