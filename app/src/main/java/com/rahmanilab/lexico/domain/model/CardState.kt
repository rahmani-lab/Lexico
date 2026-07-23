package com.rahmanilab.lexico.domain.model

/**
 * Lifecycle state of a card inside the spaced-repetition scheduler.
 *
 * Stored as a [String] in [com.rahmanilab.lexico.data.local.entity.CardScheduleEntity] so the
 * on-disk value is human readable and forward compatible.
 */
enum class CardState {
    /** Never studied. */
    NEW,

    /** Being learned for the first time, still going through the short learning steps. */
    LEARNING,

    /** Graduated to long-term review with day-scale intervals. */
    REVIEW,

    /** Was in [REVIEW] but forgotten; going back through short steps before re-graduating. */
    RELEARNING;

    companion object {
        fun fromString(value: String): CardState =
            entries.firstOrNull { it.name == value } ?: NEW
    }
}
