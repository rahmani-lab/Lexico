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

    suspend fun createDeck(
        name: String,
        description: String = "",
        languagePairId: Long = DeckEntity.DEFAULT_PAIR_ID
    ): Long =
        deckDao.insert(
            DeckEntity(
                name = name.trim(),
                description = description.trim(),
                createdAt = System.currentTimeMillis(),
                languagePairId = languagePairId
            )
        )

    suspend fun updateDeck(deck: DeckEntity) = deckDao.update(deck)

    suspend fun deleteDeck(deck: DeckEntity) = deckDao.delete(deck)
}
