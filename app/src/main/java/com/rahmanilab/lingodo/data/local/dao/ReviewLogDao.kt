package com.rahmanilab.lingodo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rahmanilab.lingodo.data.local.entity.ReviewLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewLogDao {

    @Insert
    suspend fun insert(log: ReviewLogEntity): Long

    @Insert
    suspend fun insertAll(logs: List<ReviewLogEntity>)

    @Query("SELECT * FROM review_logs")
    suspend fun getAll(): List<ReviewLogEntity>

    @Query("SELECT * FROM review_logs ORDER BY reviewedAt ASC")
    fun observeAll(): Flow<List<ReviewLogEntity>>

    @Query("SELECT * FROM review_logs WHERE reviewedAt >= :since ORDER BY reviewedAt ASC")
    fun observeSince(since: Long): Flow<List<ReviewLogEntity>>

    @Query("SELECT * FROM review_logs WHERE reviewedAt >= :since ORDER BY reviewedAt ASC")
    suspend fun getSince(since: Long): List<ReviewLogEntity>

    @Query("SELECT COUNT(*) FROM review_logs WHERE reviewedAt >= :since")
    fun observeCountSince(since: Long): Flow<Int>

    @Query("SELECT reviewedAt FROM review_logs ORDER BY reviewedAt ASC")
    suspend fun getAllReviewTimestamps(): List<Long>
}
