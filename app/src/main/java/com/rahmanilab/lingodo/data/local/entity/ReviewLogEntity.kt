package com.rahmanilab.lingodo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * An immutable record of a single review. Every review is stored from day one so that the full
 * learning history is preserved and a smarter scheduler (FSRS) can be trained on it later.
 */
@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cardId"), Index("reviewedAt")]
)
@Serializable
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val cardId: Long,
    val rating: Int,
    val reviewedAt: Long,

    val previousIntervalDays: Int,
    val nextIntervalDays: Int,

    val responseTimeMillis: Long? = null
)
