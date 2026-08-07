package com.rahmanilab.lingodo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.rahmanilab.lingodo.data.local.entity.CardEntity
import com.rahmanilab.lingodo.data.local.entity.CardLinkCrossRef
import kotlinx.coroutines.flow.Flow

/** Reads and writes the symmetric card ↔ card links behind concept clusters. */
@Dao
interface CardLinkDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(links: List<CardLinkCrossRef>)

    @Query("DELETE FROM card_links WHERE cardId = :cardId OR linkedCardId = :cardId")
    suspend fun deleteAllForCard(cardId: Long)

    @Query("DELETE FROM card_links WHERE (cardId = :a AND linkedCardId = :b) OR (cardId = :b AND linkedCardId = :a)")
    suspend fun unlink(a: Long, b: Long)

    @Query("SELECT linkedCardId FROM card_links WHERE cardId = :cardId")
    suspend fun linkedIds(cardId: Long): List<Long>

    @Query("SELECT * FROM card_links")
    suspend fun getAll(): List<CardLinkCrossRef>

    @Transaction
    @Query(
        """
        SELECT c.* FROM cards c
        INNER JOIN card_links l ON l.linkedCardId = c.id
        WHERE l.cardId = :cardId
        ORDER BY c.word COLLATE NOCASE ASC
        """
    )
    fun observeLinkedCards(cardId: Long): Flow<List<CardEntity>>

    @Query(
        """
        SELECT c.* FROM cards c
        INNER JOIN card_links l ON l.linkedCardId = c.id
        WHERE l.cardId = :cardId
        ORDER BY c.word COLLATE NOCASE ASC
        """
    )
    suspend fun linkedCards(cardId: Long): List<CardEntity>
}
