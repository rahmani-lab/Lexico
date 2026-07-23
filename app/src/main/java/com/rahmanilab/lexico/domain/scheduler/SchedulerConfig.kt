package com.rahmanilab.lexico.domain.scheduler

/**
 * Tunable constants for the built-in SM-2 style scheduler. Defaults are close to Anki's out-of-the
 * box behaviour and are deliberately conservative for a beginner-friendly vocabulary app.
 */
data class SchedulerConfig(
    /** Short steps (in minutes) a brand-new card walks through before graduating. */
    val learningStepsMinutes: List<Int> = listOf(1, 10),
    /** Short steps (in minutes) used after a lapse before returning to review. */
    val relearningStepsMinutes: List<Int> = listOf(10),

    /** Interval (days) granted when a learning card graduates with "Good". */
    val graduatingIntervalDays: Int = 1,
    /** Interval (days) granted when a learning card graduates with "Easy". */
    val easyIntervalDays: Int = 4,

    val startingEase: Double = 2.5,
    val minEase: Double = 1.3,

    val easyBonus: Double = 1.3,
    val hardMultiplier: Double = 1.2,
    val intervalModifier: Double = 1.0,

    /** Fraction of the previous interval kept when a review card is forgotten. */
    val lapseIntervalMultiplier: Double = 0.5,

    val minReviewIntervalDays: Int = 1,
    val maxIntervalDays: Int = 365 * 10,

    val easeHardDelta: Double = -0.15,
    val easeEasyDelta: Double = 0.15,
    val easeLapseDelta: Double = -0.20
)
