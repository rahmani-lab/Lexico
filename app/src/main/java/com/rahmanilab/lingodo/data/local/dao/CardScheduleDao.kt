package com.rahmanilab.lingodo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rahmanilab.lingodo.data.local.entity.CardScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(schedule: CardScheduleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<CardScheduleEntity>)

    @Query("SELECT * FROM card_schedule")
    suspend fun getAll(): List<CardScheduleEntity>

    @Query("SELECT * FROM card_schedule WHERE cardId = :cardId")
    suspend fun getByCardId(cardId: Long): CardScheduleEntity?

    @Query("SELECT COUNT(*) FROM card_schedule WHERE state != 'NEW' AND dueAt <= :now")
    fun observeDueCount(now: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM card_schedule WHERE state = 'NEW'")
    fun observeNewCount(): Flow<Int>

    // --- counts scoped to the active language pair (Home) ---

    @Query(
        """
        SELECT COUNT(*) FROM card_schedule s
        INNER JOIN cards c ON c.id = s.cardId
        INNER JOIN decks d ON d.id = c.deckId
        WHERE d.languagePairId = :pairId AND s.state != 'NEW' AND s.dueAt <= :now
        """
    )
    fun observeDueCountForPair(now: Long, pairId: Long): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM card_schedule s
        INNER JOIN cards c ON c.id = s.cardId
        INNER JOIN decks d ON d.id = c.deckId
        WHERE d.languagePairId = :pairId AND s.state = 'NEW'
        """
    )
    fun observeNewCountForPair(pairId: Long): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM card_schedule s
        INNER JOIN cards c ON c.id = s.cardId
        INNER JOIN decks d ON d.id = c.deckId
        WHERE d.languagePairId = :pairId
        """
    )
    fun observeTotalForPair(pairId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM card_schedule WHERE state = 'REVIEW'")
    fun observeLearnedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM card_schedule")
    fun observeTotalCount(): Flow<Int>

    // --- point-in-time counts for the statistics snapshot ---

    @Query("SELECT COUNT(*) FROM card_schedule WHERE state = 'REVIEW'")
    suspend fun countLearned(): Int

    @Query("SELECT COUNT(*) FROM card_schedule WHERE state = 'REVIEW' AND intervalDays >= :days")
    suspend fun countMature(days: Int): Int

    @Query("SELECT COUNT(*) FROM card_schedule")
    suspend fun countTotal(): Int

    /** New cards plus cards that are due now — everything the learner could study right now. */
    @Query("SELECT COUNT(*) FROM card_schedule WHERE state = 'NEW' OR dueAt <= :now")
    suspend fun countStudyable(now: Long): Int
}
