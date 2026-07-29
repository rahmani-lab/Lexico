package com.rahmanilab.lingodo.domain.model

import kotlinx.serialization.Serializable

/**
 * A single example sentence for a card together with its (optional) Persian translation.
 *
 * A card can hold several of these; the list is persisted as JSON inside
 * [com.rahmanilab.lingodo.data.local.entity.CardEntity] via a Room type converter.
 */
@Serializable
data class Example(
    val text: String,
    val translation: String = ""
)
