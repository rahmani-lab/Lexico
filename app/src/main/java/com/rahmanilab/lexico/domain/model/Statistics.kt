package com.rahmanilab.lexico.domain.model

import java.time.LocalDate

/** Number of reviews done on a given calendar day. */
data class DailyReviewCount(
    val date: LocalDate,
    val count: Int
)

/** A snapshot of the learner's progress, shown on the Statistics screen. */
data class Statistics(
    val totalReviews: Int = 0,
    val reviewsToday: Int = 0,
    val reviewsThisWeek: Int = 0,
    val correctRatePercent: Int = 0,
    val totalCards: Int = 0,
    val learnedCount: Int = 0,
    val matureCount: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val perDay: List<DailyReviewCount> = emptyList()
)
