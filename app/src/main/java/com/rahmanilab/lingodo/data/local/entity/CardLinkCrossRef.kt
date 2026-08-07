package com.rahmanilab.lingodo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

/**
 * Links two cards into a "concept cluster" of related or easily confused words (fact / truth /
 * trust). Links are stored symmetrically — both (a→b) and (b→a) rows are written — so a lookup from
 * either side is a single indexed query.
 *
 * Review and practice use these links to surface clustered cards together, letting the learner
 * contrast the confusable meanings side by side instead of meeting them weeks apart.
 */
@Serializable
@Entity(
    tableName = "card_links",
    primaryKeys = ["cardId", "linkedCardId"],
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedCardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cardId"), Index("linkedCardId")]
)
data class CardLinkCrossRef(
    val cardId: Long,
    val linkedCardId: Long
)
