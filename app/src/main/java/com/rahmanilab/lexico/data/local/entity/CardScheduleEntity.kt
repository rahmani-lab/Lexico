package com.rahmanilab.lexico.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * The spaced-repetition state of a card: when it is next due and everything the scheduler needs.
 *
 * [difficulty] and [stability] are intentionally nullable and unused by the current SM-2 style
 * scheduler; they are reserved so an FSRS scheduler can be added later without a destructive
 * migration or any loss of the stored review history.
 */
@Entity(
    tableName = "card_schedule",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dueAt"), Index("state")]
)
@Serializable
data class CardScheduleEntity(
    @PrimaryKey
    val cardId: Long,

    val state: String = "NEW",
    val dueAt: Long,

    val repetitions: Int = 0,
    val lapses: Int = 0,
    val intervalDays: Int = 0,

    /** SM-2 ease factor. Kept &ge; 1.3. */
    val easeFactor: Double = 2.5,
    /** Index into the (re)learning steps while the card is in LEARNING/RELEARNING. */
    val learningStepIndex: Int = 0,
    val lastReviewedAt: Long? = null,

    // --- Reserved for a future FSRS scheduler ---
    val difficulty: Double? = null,
    val stability: Double? = null
)
