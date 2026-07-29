package com.rahmanilab.lingodo.domain.scheduler

import com.rahmanilab.lingodo.domain.model.CardState
import com.rahmanilab.lingodo.domain.model.Rating

/**
 * An FSRS-based [Scheduler].
 *
 * The FSRS equations (in [FsrsParameters]) drive difficulty, stability and the day-scale interval.
 * A single short relearning step is kept for "Again" so a forgotten card reappears within the same
 * study session (this mirrors how Anki layers learning steps on top of FSRS); on the next successful
 * answer the card graduates to the FSRS-computed interval.
 */
class FsrsScheduler(
    private val params: FsrsParameters = FsrsParameters(),
    private val relearnStepMinutes: Int = 10
) : Scheduler {

    override fun schedule(current: SchedulingState, rating: Rating, now: Long): SchedulingState {
        val grade = rating.value
        val isNew = current.state == CardState.NEW || current.stability == null

        val difficulty: Double
        val stability: Double
        if (isNew) {
            difficulty = params.initDifficulty(grade)
            stability = params.initStability(grade)
        } else {
            val prevStability = current.stability ?: params.initStability(grade)
            val prevDifficulty = current.difficulty ?: params.initDifficulty(3)
            val retrievability = params.retrievability(elapsedDays(current, now), prevStability)
            difficulty = params.nextDifficulty(prevDifficulty, grade)
            stability = if (grade == Rating.AGAIN.value) {
                params.nextStabilityOnForget(difficulty, prevStability, retrievability)
            } else {
                params.nextStabilityOnRecall(difficulty, prevStability, retrievability, grade)
            }
        }

        val intervalDays = params.nextInterval(stability)
        val lapsed = grade == Rating.AGAIN.value && current.state == CardState.REVIEW

        val base = current.copy(
            difficulty = difficulty,
            stability = stability,
            lastReviewedAt = now,
            repetitions = if (grade == Rating.AGAIN.value) current.repetitions else current.repetitions + 1,
            lapses = if (lapsed) current.lapses + 1 else current.lapses,
            easeFactor = current.easeFactor
        )

        return if (grade == Rating.AGAIN.value) {
            base.copy(
                state = CardState.RELEARNING,
                learningStepIndex = 0,
                intervalDays = intervalDays,
                dueAt = now + relearnStepMinutes * 60_000L
            )
        } else {
            base.copy(
                state = CardState.REVIEW,
                learningStepIndex = 0,
                intervalDays = intervalDays,
                dueAt = now + intervalDays * 86_400_000L
            )
        }
    }

    private fun elapsedDays(current: SchedulingState, now: Long): Double {
        val last = current.lastReviewedAt ?: return 0.0
        return ((now - last).coerceAtLeast(0L)) / 86_400_000.0
    }
}
