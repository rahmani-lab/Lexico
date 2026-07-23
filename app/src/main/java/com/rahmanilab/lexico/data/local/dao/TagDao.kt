package com.rahmanilab.lexico.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rahmanilab.lexico.data.local.entity.CardTagCrossRef
import com.rahmanilab.lexico.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): TagEntity?

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags")
    suspend fun getAllTags(): List<TagEntity>

    @Query("SELECT * FROM card_tag_cross_ref")
    suspend fun getAllCrossRefs(): List<CardTagCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTags(tags: List<TagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCrossRefs(refs: List<CardTagCrossRef>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(ref: CardTagCrossRef)

    @Query("DELETE FROM card_tag_cross_ref WHERE cardId = :cardId")
    suspend fun clearTagsForCard(cardId: Long)

    /** Remove tags that are no longer referenced by any card. */
    @Query("DELETE FROM tags WHERE id NOT IN (SELECT DISTINCT tagId FROM card_tag_cross_ref)")
    suspend fun deleteOrphanTags()
}
