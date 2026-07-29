package com.rahmanilab.lingodo.domain.model

/**
 * The four answers a learner can give while reviewing a card.
 *
 * The numeric [value] is what gets persisted in [com.rahmanilab.lingodo.data.local.entity.ReviewLogEntity]
 * so that the learning history stays stable even if the scheduler implementation changes later
 * (for example when migrating from the built-in SM-2 scheduler to FSRS).
 */
enum class Rating(val value: Int) {
    /** "I didn't know it." Resets the card into (re)learning. */
    AGAIN(1),

    /** "That was hard." Correct, but the interval grows slowly. */
    HARD(2),

    /** "I knew it." The normal, expected answer. */
    GOOD(3),

    /** "That was easy." The interval grows faster than usual. */
    EASY(4);

    val isCorrect: Boolean get() = this != AGAIN

    companion object {
        fun fromValue(value: Int): Rating = entries.firstOrNull { it.value == value } ?: GOOD
    }
}
