package com.rahmanilab.lingodo.domain.scheduler

import com.rahmanilab.lingodo.domain.model.CardState
import com.rahmanilab.lingodo.domain.model.Rating
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A pragmatic SM-2 style scheduler with Anki-like learning steps.
 *
 * It is deterministic and side-effect free, which makes it trivial to unit test. When the app is
 * ready for FSRS this class can be replaced without changing the persisted schema (the reserved
 * `difficulty`/`stability` columns and the full review log are already in place).
 */
class Sm2Scheduler(
    private val config: SchedulerConfig = SchedulerConfig()
) : Scheduler {

    override fun schedule(current: SchedulingState, rating: Rating, now: Long): SchedulingState =
        when (current.state) {
            CardState.NEW, CardState.LEARNING -> scheduleLearning(current, rating, now, config.learningStepsMinutes)
            CardState.RELEARNING -> scheduleLearning(current, rating, now, config.relearningStepsMinutes)
            CardState.REVIEW -> scheduleReview(current, rating, now)
        }

    private fun scheduleLearning(
        current: SchedulingState,
        rating: Rating,
        now: Long,
        steps: List<Int>
    ): SchedulingState {
        val relearning = current.state == CardState.RELEARNING || current.state == CardState.REVIEW
        val base = current.copy(lastReviewedAt = now)

        fun stayAt(index: Int): SchedulingState {
            val clamped = index.coerceIn(0, max(0, steps.lastIndex))
            val minutes = steps.getOrElse(clamped) { config.graduatingIntervalDays * 24 * 60 }
            return base.copy(
                state = if (relearning) CardState.RELEARNING else CardState.LEARNING,
                learningStepIndex = clamped,
                dueAt = now + minutesToMillis(minutes)
            )
        }

        fun graduate(intervalDays: Int): SchedulingState = base.copy(
            state = CardState.REVIEW,
            learningStepIndex = 0,
            intervalDays = intervalDays.coerceIn(config.minReviewIntervalDays, config.maxIntervalDays),
            repetitions = current.repetitions + 1,
            dueAt = now + daysToMillis(intervalDays)
        )

        return when (rating) {
            Rating.AGAIN -> stayAt(0)
            Rating.HARD -> stayAt(current.learningStepIndex)
            Rating.GOOD -> {
                val next = current.learningStepIndex + 1
                if (next >= steps.size) graduate(graduatingInterval(relearning, current)) else stayAt(next)
            }
            Rating.EASY -> graduate(if (relearning) graduatingInterval(true, current) else config.easyIntervalDays)
        }
    }

    /** Interval used when a card graduates out of the (re)learning steps. */
    private fun graduatingInterval(relearning: Boolean, current: SchedulingState): Int =
        if (relearning) max(config.minReviewIntervalDays, current.intervalDays)
        else config.graduatingIntervalDays

    private fun scheduleReview(current: SchedulingState, rating: Rating, now: Long): SchedulingState {
        val base = current.copy(lastReviewedAt = now)
        val interval = max(config.minReviewIntervalDays, current.intervalDays)

        return when (rating) {
            Rating.AGAIN -> {
                // Lapse: drop into relearning and remember a reduced target interval to resume with.
                val reduced = clampInterval((interval * config.lapseIntervalMultiplier).roundToInt())
                base.copy(
                    state = CardState.RELEARNING,
                    lapses = current.lapses + 1,
                    easeFactor = clampEase(current.easeFactor + config.easeLapseDelta),
                    learningStepIndex = 0,
                    intervalDays = reduced,
                    dueAt = now + minutesToMillis(config.relearningStepsMinutes.firstOrNull() ?: 10)
                )
            }
            Rating.HARD -> {
                val next = grow(interval, config.hardMultiplier)
                base.copy(
                    state = CardState.REVIEW,
                    easeFactor = clampEase(current.easeFactor + config.easeHardDelta),
                    intervalDays = next,
                    repetitions = current.repetitions + 1,
                    dueAt = now + daysToMillis(next)
                )
            }
            Rating.GOOD -> {
                val next = grow(interval, current.easeFactor)
                base.copy(
                    state = CardState.REVIEW,
                    intervalDays = next,
                    repetitions = current.repetitions + 1,
                    dueAt = now + daysToMillis(next)
                )
            }
            Rating.EASY -> {
                val next = grow(interval, current.easeFactor * config.easyBonus)
                base.copy(
                    state = CardState.REVIEW,
                    easeFactor = clampEase(current.easeFactor + config.easeEasyDelta),
                    intervalDays = next,
                    repetitions = current.repetitions + 1,
                    dueAt = now + daysToMillis(next)
                )
            }
        }
    }

    /** Multiply the interval, apply the global modifier, and make sure it always grows by ≥ 1 day. */
    private fun grow(interval: Int, multiplier: Double): Int =
        clampInterval(max(interval + 1, (interval * multiplier * config.intervalModifier).roundToInt()))

    private fun clampInterval(days: Int): Int =
        days.coerceIn(config.minReviewIntervalDays, config.maxIntervalDays)

    private fun clampEase(ease: Double): Double = max(config.minEase, ease)

    private companion object {
        fun minutesToMillis(minutes: Int): Long = minutes * 60_000L
        fun daysToMillis(days: Int): Long = days * 86_400_000L
    }
}
