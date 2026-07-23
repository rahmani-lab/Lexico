package com.rahmanilab.lexico.data.repository

import com.rahmanilab.lexico.data.local.dao.DeckDao
import com.rahmanilab.lexico.data.local.entity.DeckEntity
import com.rahmanilab.lexico.data.local.projection.DeckStats
import kotlinx.coroutines.flow.Flow

class DeckRepository(private val deckDao: DeckDao) {

    fun observeDecks(): Flow<List<DeckEntity>> = deckDao.observeAll()

    fun observeDeckStats(now: Long = System.currentTimeMillis()): Flow<List<DeckStats>> =
        deckDao.observeDeckStats(now)

    fun observeDeck(id: Long): Flow<DeckEntity?> = deckDao.observeById(id)

    suspend fun getDeck(id: Long): DeckEntity? = deckDao.getById(id)

    suspend fun deckCount(): Int = deckDao.count()

    suspend fun createDeck(name: String, description: String = ""): Long =
        deckDao.insert(
            DeckEntity(
                name = name.trim(),
                description = description.trim(),
                createdAt = System.currentTimeMillis()
            )
        )

    suspend fun updateDeck(deck: DeckEntity) = deckDao.update(deck)

    suspend fun deleteDeck(deck: DeckEntity) = deckDao.delete(deck)
}
