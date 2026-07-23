package com.rahmanilab.lexico.domain.scheduler

import com.rahmanilab.lexico.domain.model.CardState

/**
 * An immutable snapshot of everything the [Scheduler] reads and writes. It maps 1:1 onto the
 * persistent [com.rahmanilab.lexico.data.local.entity.CardScheduleEntity] but keeps the scheduling
 * logic free of any Room / Android dependency so it can be unit tested in isolation.
 */
data class SchedulingState(
    val state: CardState,
    val dueAt: Long,
    val intervalDays: Int,
    val easeFactor: Double,
    val repetitions: Int,
    val lapses: Int,
    val learningStepIndex: Int,
    val lastReviewedAt: Long?,
    /** FSRS memory-difficulty (1..10); null until an FSRS review runs. Ignored by [Sm2Scheduler]. */
    val difficulty: Double? = null,
    /** FSRS memory-stability in days; null until an FSRS review runs. Ignored by [Sm2Scheduler]. */
    val stability: Double? = null
) {
    companion object {
        /** The state of a freshly created, never-studied card that is due immediately. */
        fun newCard(now: Long, startingEase: Double = 2.5): SchedulingState = SchedulingState(
            state = CardState.NEW,
            dueAt = now,
            intervalDays = 0,
            easeFactor = startingEase,
            repetitions = 0,
            lapses = 0,
            learningStepIndex = 0,
            lastReviewedAt = null,
            difficulty = null,
            stability = null
        )
    }
}
