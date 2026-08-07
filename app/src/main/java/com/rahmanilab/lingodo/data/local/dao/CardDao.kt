package com.rahmanilab.lingodo.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.rahmanilab.lingodo.data.local.entity.CardEntity
import com.rahmanilab.lingodo.data.local.relation.CardWithDetails
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
           OR meaning LIKE '%' || :query || '%'
           OR englishDefinition LIKE '%' || :query || '%'
           OR notes LIKE '%' || :query || '%'
           OR source LIKE '%' || :query || '%'
        ORDER BY word COLLATE NOCASE ASC
        """
    )
    fun searchWithDetails(query: String): Flow<List<CardWithDetails>>

    /** Cards due for review now, optionally limited to a deck and/or the active language pair. */
    @Transaction
    @Query(
        """
        SELECT c.* FROM cards c
        INNER JOIN card_schedule s ON s.cardId = c.id
        INNER JOIN decks d ON d.id = c.deckId
        WHERE (:deckId IS NULL OR c.deckId = :deckId)
          AND (:pairId IS NULL OR d.languagePairId = :pairId)
          AND s.state != 'NEW'
          AND s.dueAt <= :now
        ORDER BY s.dueAt ASC
        """
    )
    suspend fun getDueCards(now: Long, deckId: Long?, pairId: Long?): List<CardWithDetails>

    /** Brand-new cards, oldest first, capped at [limit]. */
    @Transaction
    @Query(
        """
        SELECT c.* FROM cards c
        INNER JOIN card_schedule s ON s.cardId = c.id
        INNER JOIN decks d ON d.id = c.deckId
        WHERE (:deckId IS NULL OR c.deckId = :deckId)
          AND (:pairId IS NULL OR d.languagePairId = :pairId)
          AND s.state = 'NEW'
        ORDER BY c.createdAt ASC
        LIMIT :limit
        """
    )
    suspend fun getNewCards(deckId: Long?, limit: Int, pairId: Long?): List<CardWithDetails>

    /** Due cards across a set of decks (a parent deck plus its lessons). */
    @Transaction
    @Query(
        """
        SELECT c.* FROM cards c
        INNER JOIN card_schedule s ON s.cardId = c.id
        INNER JOIN decks d ON d.id = c.deckId
        WHERE c.deckId IN (:deckIds)
          AND (:pairId IS NULL OR d.languagePairId = :pairId)
          AND s.state != 'NEW'
          AND s.dueAt <= :now
        ORDER BY s.dueAt ASC
        """
    )
    suspend fun getDueCardsIn(now: Long, deckIds: List<Long>, pairId: Long?): List<CardWithDetails>

    /** Brand-new cards across a set of decks (a parent deck plus its lessons), oldest first. */
    @Transaction
    @Query(
        """
        SELECT c.* FROM cards c
        INNER JOIN card_schedule s ON s.cardId = c.id
        INNER JOIN decks d ON d.id = c.deckId
        WHERE c.deckId IN (:deckIds)
          AND (:pairId IS NULL OR d.languagePairId = :pairId)
          AND s.state = 'NEW'
        ORDER BY c.createdAt ASC
        LIMIT :limit
        """
    )
    suspend fun getNewCardsIn(deckIds: List<Long>, limit: Int, pairId: Long?): List<CardWithDetails>

    /** Due cards across every deck except those the user excluded from global review. */
    @Transaction
    @Query(
        """
        SELECT c.* FROM cards c
        INNER JOIN card_schedule s ON s.cardId = c.id
        INNER JOIN decks d ON d.id = c.deckId
        WHERE c.deckId NOT IN (:excludedDeckIds)
          AND (:pairId IS NULL OR d.languagePairId = :pairId)
          AND s.state != 'NEW'
          AND s.dueAt <= :now
        ORDER BY s.dueAt ASC
        """
    )
    suspend fun getDueCardsExcluding(
        now: Long,
        excludedDeckIds: List<Long>,
        pairId: Long?
    ): List<CardWithDetails>

    /** New cards across every deck except those the user excluded from global review. */
    @Transaction
    @Query(
        """
        SELECT c.* FROM cards c
        INNER JOIN card_schedule s ON s.cardId = c.id
        INNER JOIN decks d ON d.id = c.deckId
        WHERE c.deckId NOT IN (:excludedDeckIds)
          AND (:pairId IS NULL OR d.languagePairId = :pairId)
          AND s.state = 'NEW'
        ORDER BY c.createdAt ASC
        LIMIT :limit
        """
    )
    suspend fun getNewCardsExcluding(
        excludedDeckIds: List<Long>,
        limit: Int,
        pairId: Long?
    ): List<CardWithDetails>

    // --- Adaptive practice (troublesome / mastered words in the active pair) ---

    @Transaction
    @Query(
        """
        SELECT c.* FROM cards c
        INNER JOIN card_schedule s ON s.cardId = c.id
        INNER JOIN decks d ON d.id = c.deckId
        WHERE d.languagePairId = :pairId
          AND (s.lapses >= 2 OR s.easeFactor < 2.0 OR (s.stability IS NOT NULL AND s.stability < 3.0))
        ORDER BY s.lapses DESC, s.easeFactor ASC
        LIMIT :limit
        """
    )
    suspend fun getTroublesomeCards(pairId: Long, limit: Int): List<CardWithDetails>

    @Transaction
    @Query(
        """
        SELECT c.* FROM cards c
        INNER JOIN card_schedule s ON s.cardId = c.id
        INNER JOIN decks d ON d.id = c.deckId
        WHERE d.languagePairId = :pairId AND s.state = 'REVIEW' AND s.intervalDays >= :days
        ORDER BY s.intervalDays DESC
        LIMIT :limit
        """
    )
    suspend fun getMasteredCards(pairId: Long, days: Int, limit: Int): List<CardWithDetails>

    @Query(
        """
        SELECT c.id FROM cards c
        INNER JOIN decks d ON d.id = c.deckId
        WHERE d.languagePairId = :pairId AND c.word = :word COLLATE NOCASE
        LIMIT 1
        """
    )
    suspend fun findCardIdByWordInPair(pairId: Long, word: String): Long?

    // --- Duplicate detection ---

    @Query("SELECT COUNT(*) FROM cards WHERE word = :word COLLATE NOCASE")
    suspend fun countByWord(word: String): Int

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND word = :word COLLATE NOCASE")
    suspend fun countByWordInDeck(deckId: Long, word: String): Int
}
