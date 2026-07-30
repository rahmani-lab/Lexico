package com.rahmanilab.lingodo.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rahmanilab.lingodo.data.local.entity.DeckEntity
import com.rahmanilab.lingodo.data.local.projection.DeckStats
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deck: DeckEntity): Long

    @Update
    suspend fun update(deck: DeckEntity)

    @Delete
    suspend fun delete(deck: DeckEntity)

    @Query("SELECT * FROM decks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE languagePairId = :pairId ORDER BY createdAt DESC")
    fun observeByPair(pairId: Long): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE id = :id")
    fun observeById(id: Long): Flow<DeckEntity?>

    @Query("SELECT * FROM decks WHERE id = :id")
    suspend fun getById(id: Long): DeckEntity?

    @Query("SELECT COUNT(*) FROM decks")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM decks WHERE languagePairId = :pairId")
    suspend fun countByPair(pairId: Long): Int

    @Query("SELECT * FROM decks")
    suspend fun getAll(): List<DeckEntity>

    /** Direct children ("lessons") of a parent deck ("book"), newest first. */
    @Query("SELECT * FROM decks WHERE parentId = :parentId ORDER BY createdAt DESC")
    suspend fun getChildren(parentId: Long): List<DeckEntity>

    /** Ids of a parent deck's direct children, for building a combined study queue. */
    @Query("SELECT id FROM decks WHERE parentId = :parentId")
    suspend fun childDeckIds(parentId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(decks: List<DeckEntity>)

    /**
     * One grouped query returning per-deck counts. New cards, due cards (excluding new) and
     * graduated ("learned") cards are counted separately.
     */
    @Query(
        """
        SELECT
            d.id AS id,
            d.name AS name,
            d.description AS description,
            d.createdAt AS createdAt,
            d.languagePairId AS languagePairId,
            d.parentId AS parentId,
            COUNT(c.id) AS total,
            COALESCE(SUM(CASE WHEN s.state = 'NEW' THEN 1 ELSE 0 END), 0) AS newCount,
            COALESCE(SUM(CASE WHEN s.state != 'NEW' AND s.dueAt <= :now THEN 1 ELSE 0 END), 0) AS dueCount,
            COALESCE(SUM(CASE WHEN s.state = 'REVIEW' THEN 1 ELSE 0 END), 0) AS learnedCount
        FROM decks d
        LEFT JOIN cards c ON c.deckId = d.id
        LEFT JOIN card_schedule s ON s.cardId = c.id
        WHERE d.languagePairId = :pairId
        GROUP BY d.id
        ORDER BY d.createdAt DESC
        """
    )
    fun observeDeckStats(now: Long, pairId: Long): Flow<List<DeckStats>>
}
