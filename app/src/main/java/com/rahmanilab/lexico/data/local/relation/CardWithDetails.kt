package com.rahmanilab.lexico.data.local.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.rahmanilab.lexico.data.local.entity.CardEntity
import com.rahmanilab.lexico.data.local.entity.CardScheduleEntity
import com.rahmanilab.lexico.data.local.entity.CardTagCrossRef
import com.rahmanilab.lexico.data.local.entity.TagEntity

/**
 * A card together with its schedule and tags. Loaded through Room `@Relation`s so callers never
 * have to issue extra queries.
 */
data class CardWithDetails(
    @Embedded val card: CardEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "cardId"
    )
    val schedule: CardScheduleEntity?,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = CardTagCrossRef::class,
            parentColumn = "cardId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity> = emptyList()
)
