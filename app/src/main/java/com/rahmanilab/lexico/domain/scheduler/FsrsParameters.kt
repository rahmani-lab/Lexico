package com.rahmanilab.lexico.domain.scheduler

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Parameters and core equations of the **FSRS** (Free Spaced Repetition Scheduler) algorithm,
 * using the published FSRS-5 default weights.
 *
 * FSRS models memory with three quantities:
 *  - **D** (difficulty, 1..10),
 *  - **S** (stability, in days — the time for recall probability to fall to 90%),
 *  - **R** (retrievability — the current probability of recall).
 *
 * See the Open Spaced Repetition project: https://github.com/open-spaced-repetition
 */
data class FsrsParameters(
    val w: DoubleArray = DEFAULT_WEIGHTS,
    val requestRetention: Double = 0.9,
    val maxIntervalDays: Int = 365 * 10
) {
    /** Retrievability after [elapsedDays] with the given stability. */
    fun retrievability(elapsedDays: Double, stability: Double): Double =
        (1.0 + FACTOR * elapsedDays / stability).pow(DECAY)

    fun initStability(grade: Int): Double =
        max(0.1, w[(grade - 1).coerceIn(0, 3)])

    fun initDifficulty(grade: Int): Double =
        clampDifficulty(w[4] - exp(w[5] * (grade - 1)) + 1.0)

    fun nextDifficulty(difficulty: Double, grade: Int): Double {
        val delta = -w[6] * (grade - 3)
        val damped = difficulty + delta * (10.0 - difficulty) / 9.0
        // Mean-reversion toward the "Easy" initial difficulty.
        return clampDifficulty(w[7] * initDifficulty(4) + (1.0 - w[7]) * damped)
    }

    fun nextStabilityOnRecall(difficulty: Double, stability: Double, retrievability: Double, grade: Int): Double {
        val hardPenalty = if (grade == 2) w[15] else 1.0
        val easyBonus = if (grade == 4) w[16] else 1.0
        val growth = exp(w[8]) *
            (11.0 - difficulty) *
            stability.pow(-w[9]) *
            (exp(w[10] * (1.0 - retrievability)) - 1.0) *
            hardPenalty *
            easyBonus
        return stability * (1.0 + growth)
    }

    fun nextStabilityOnForget(difficulty: Double, stability: Double, retrievability: Double): Double {
        val forgotten = w[11] *
            difficulty.pow(-w[12]) *
            ((stability + 1.0).pow(w[13]) - 1.0) *
            exp(w[14] * (1.0 - retrievability))
        // A lapse can never make a memory more stable than it already was.
        return min(forgotten, stability)
    }

    /** Days until the card should next be seen, given its stability and the target retention. */
    fun nextInterval(stability: Double): Int {
        val raw = (stability / FACTOR) * (requestRetention.pow(1.0 / DECAY) - 1.0)
        return raw.roundToInt().coerceIn(1, maxIntervalDays)
    }

    private fun clampDifficulty(d: Double): Double = d.coerceIn(1.0, 10.0)

    override fun equals(other: Any?): Boolean =
        this === other || (other is FsrsParameters && w.contentEquals(other.w) &&
            requestRetention == other.requestRetention && maxIntervalDays == other.maxIntervalDays)

    override fun hashCode(): Int =
        w.contentHashCode() * 31 + requestRetention.hashCode()

    companion object {
        const val DECAY = -0.5
        const val FACTOR = 19.0 / 81.0

        /** FSRS-5 default weights. */
        val DEFAULT_WEIGHTS = doubleArrayOf(
            0.40255, 1.18385, 3.173, 15.69105, 7.1949, 0.5345, 1.4604, 0.0046,
            1.54575, 0.1192, 1.01925, 1.9395, 0.11, 0.29605, 2.2698, 0.2315,
            2.9898, 0.51655, 0.6621
        )
    }
}
