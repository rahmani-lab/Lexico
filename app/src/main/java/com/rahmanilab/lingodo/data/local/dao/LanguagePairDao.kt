package com.rahmanilab.lingodo.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rahmanilab.lingodo.data.local.entity.LanguagePairEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LanguagePairDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(pair: LanguagePairEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pairs: List<LanguagePairEntity>)

    @Delete
    suspend fun delete(pair: LanguagePairEntity)

    @Query("SELECT * FROM language_pairs ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<LanguagePairEntity>>

    @Query("SELECT * FROM language_pairs")
    suspend fun getAll(): List<LanguagePairEntity>

    @Query("SELECT * FROM language_pairs WHERE id = :id")
    suspend fun getById(id: Long): LanguagePairEntity?

    @Query("SELECT * FROM language_pairs WHERE sourceCode = :source AND targetCode = :target LIMIT 1")
    suspend fun find(source: String, target: String): LanguagePairEntity?

    @Query("SELECT COUNT(*) FROM language_pairs")
    suspend fun count(): Int
}
