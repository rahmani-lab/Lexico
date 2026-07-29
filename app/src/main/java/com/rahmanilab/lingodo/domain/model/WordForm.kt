package com.rahmanilab.lingodo.domain.model

import kotlinx.serialization.Serializable

/**
 * A grammatical form or derivative of a word — e.g. label "past" / form "ran", label "plural" /
 * form "children", label "comparative" / form "bigger", or a word-family member like label "noun" /
 * form "creation" for the verb "create".
 *
 * A card holds a list of these; it is persisted as JSON on [com.rahmanilab.lingodo.data.local.entity.CardEntity]
 * via a Room type converter, and can be produced automatically by the auto-fill engine.
 */
@Serializable
data class WordForm(
    val label: String,
    val form: String
)
