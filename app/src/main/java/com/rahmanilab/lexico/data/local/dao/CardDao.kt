package com.rahmanilab.lexico.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.rahmanilab.lexico.data.local.entity.CardEntity
import com.rahmanilab.lexico.data.local.relation.CardWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: CardEntity): Long

    @Update
    suspend fun update(card: CardEntity)

    @Delete
    suspend fun delete(card: CardEntity)

    @Query("DELETE FROM cards WHERE id = :cardId")
    suspend fun deleteById(cardId: Long)

    @Query("SELECT * FROM cards WHERE id = :cardId")
    suspend fun getById(cardId: Long): CardEntity?

    @Query("SELECT * FROM cards")
    suspend fun getAllCards(): List<CardEntity>

    @Transaction
    @Query("SELECT * FROM cards ORDER BY word COLLATE NOCASE ASC")
    suspend fun getAllWithDetails(): List<CardWithDetails>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<CardEntity>)

    @Transaction
    @Query("SELECT * FROM cards WHERE id = :cardId")
    fun observeCardWithDetails(cardId: Long): Flow<CardWithDetails?>

    @Transaction
    @Query("SELECT * FROM cards WHERE id = :cardId")
    suspend fun getCardWithDetails(cardId: Long): CardWithDetails?

    @Transaction
    @Query("SELECT * FROM cards ORDER BY word COLLATE NOCASE ASC")
    fun observeAllWithDetails(): Flow<List<CardWithDetails>>

    @Transaction
    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY word COLLATE NOCASE ASC")
    fun observeCardsForDeck(deckId: Long): Flow<List<CardWithDetails>>

    /** Full text-ish search across the word, both definitions, notes and source. */
    @Transaction
    @Query(
        """
        SELECT * FROM cards
        WHERE word LIKE '%' || :query || '%'
           OR persianMeaning LIKE '%' || :query || '%'
           OR englishDefinition LIKE '%' || :query || '%'
           OR notes LIKE '%' || :query || '%'
           OR source LIKE '%' || :query || '%'
        ORDER BY word COLLATE NOCASE ASC
        """
    )
    fun searchWithDetails(query: String): Flow<List<CardWithDetails>>

    /** Cards that are due for review right now (everything except brand new cards). */
    @Transaction
    @Query(
        """
        SELECT c.* FROM cards c
        INNER JOIN card_schedule s ON s.cardId = c.id
        WHERE (:deckId IS NULL OR c.deckId = :deckId)
          AND s.state != 'NEW'
          AND s.dueAt <= :now
        ORDER BY s.dueAt ASC
        """
    )
    suspend fun getDueCards(now: Long, deckId: Long?): List<CardWithDetails>

    /** Brand-new cards, oldest first, capped at [limit]. */
    @Transaction
    @Query(
        """
        SELECT c.* FROM cards c
        INNER JOIN card_schedule s ON s.cardId = c.id
        WHERE (:deckId IS NULL OR c.deckId = :deckId)
          AND s.state = 'NEW'
        ORDER BY c.createdAt ASC
        LIMIT :limit
        """
    )
    suspend fun getNewCards(deckId: Long?, limit: Int): List<CardWithDetails>

    // --- Duplicate detection ---

    @Query("SELECT COUNT(*) FROM cards WHERE word = :word COLLATE NOCASE")
    suspend fun countByWord(word: String): Int

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND word = :word COLLATE NOCASE")
    suspend fun countByWordInDeck(deckId: Long, word: String): Int
}
