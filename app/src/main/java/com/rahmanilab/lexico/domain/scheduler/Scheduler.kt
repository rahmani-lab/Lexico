package com.rahmanilab.lexico.domain.scheduler

import com.rahmanilab.lexico.domain.model.Rating

/**
 * Turns a review answer into the card's next scheduling state.
 *
 * This is intentionally a small, swappable interface: the app ships with [Sm2Scheduler] today, but
 * an FSRS implementation can be dropped in later without touching the rest of the code because the
 * complete review history is always persisted.
 */
interface Scheduler {

    /** Compute the next state after answering [rating] at [now] (epoch millis). */
    fun schedule(current: SchedulingState, rating: Rating, now: Long): SchedulingState

    /**
     * Preview the outcome for every rating, so the review UI can label each button with the
     * interval it would produce.
     */
    fun preview(current: SchedulingState, now: Long): Map<Rating, SchedulingState> =
        Rating.entries.associateWith { schedule(current, it, now) }
}
