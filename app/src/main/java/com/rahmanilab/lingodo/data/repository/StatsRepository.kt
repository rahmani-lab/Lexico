package com.rahmanilab.lingodo.data.repository

import com.rahmanilab.lingodo.data.local.LingoDoDatabase
import com.rahmanilab.lingodo.domain.model.DailyReviewCount
import com.rahmanilab.lingodo.domain.model.Rating
import com.rahmanilab.lingodo.domain.model.Statistics
import com.rahmanilab.lingodo.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlin.math.max
import kotlin.math.roundToInt

class StatsRepository(private val db: LingoDoDatabase) {

    private val reviewLogDao get() = db.reviewLogDao()
    private val scheduleDao get() = db.cardScheduleDao()

    // --- live counts for the Home screen ---

    fun observeReviewsToday(): Flow<Int> = reviewLogDao.observeCountSince(DateUtils.startOfToday())

    fun observeDueCount(): Flow<Int> = scheduleDao.observeDueCount(System.currentTimeMillis())

    fun observeNewCount(): Flow<Int> = scheduleDao.observeNewCount()

    // --- Home counts scoped to the active language pair ---

    fun observeDueCountForPair(pairId: Long): Flow<Int> =
        scheduleDao.observeDueCountForPair(System.currentTimeMillis(), pairId)

    fun observeNewCountForPair(pairId: Long): Flow<Int> = scheduleDao.observeNewCountForPair(pairId)

    fun observeTotalCardsForPair(pairId: Long): Flow<Int> = scheduleDao.observeTotalForPair(pairId)

    fun observeLearnedCount(): Flow<Int> = scheduleDao.observeLearnedCount()

    fun observeTotalCards(): Flow<Int> = scheduleDao.observeTotalCount()

    /** Compute the full statistics snapshot over the last [days] days. */
    suspend fun computeStatistics(days: Int = 30): Statistics {
        val logs = reviewLogDao.getSince(0L)
        val today = DateUtils.today()
        val todayEpochDay = today.toEpochDay()

        val total = logs.size
        val correct = logs.count { it.rating != Rating.AGAIN.value }
        val correctRate = if (total > 0) (correct * 100.0 / total).roundToInt() else 0

        val startToday = DateUtils.startOfToday()
        val reviewsToday = logs.count { it.reviewedAt >= startToday }
        val startWeek = DateUtils.startOfDaysAgo(6)
        val reviewsThisWeek = logs.count { it.reviewedAt >= startWeek }

        val countsByDay = logs.groupingBy { DateUtils.epochDay(it.reviewedAt) }.eachCount()
        val perDay = (0 until days).map { offset ->
            val date = today.minusDays((days - 1 - offset).toLong())
            DailyReviewCount(date = date, count = countsByDay[date.toEpochDay()] ?: 0)
        }

        val daySet = countsByDay.keys
        return Statistics(
            totalReviews = total,
            reviewsToday = reviewsToday,
            reviewsThisWeek = reviewsThisWeek,
            correctRatePercent = correctRate,
            totalCards = scheduleDao.countTotal(),
            learnedCount = scheduleDao.countLearned(),
            matureCount = scheduleDao.countMature(MATURE_THRESHOLD_DAYS),
            currentStreak = currentStreak(daySet, todayEpochDay),
            longestStreak = longestStreak(daySet),
            perDay = perDay
        )
    }

    private fun currentStreak(daySet: Set<Long>, todayEpochDay: Long): Int {
        if (daySet.isEmpty()) return 0
        // A streak is still "alive" today until midnight even if today has no review yet.
        var day = if (todayEpochDay in daySet) todayEpochDay else todayEpochDay - 1
        if (day !in daySet) return 0
        var streak = 0
        while (day in daySet) {
            streak++
            day--
        }
        return streak
    }

    private fun longestStreak(daySet: Set<Long>): Int {
        if (daySet.isEmpty()) return 0
        val sorted = daySet.toSortedSet()
        var longest = 1
        var run = 1
        var prev: Long? = null
        for (d in sorted) {
            run = if (prev != null && d == prev + 1) run + 1 else 1
            longest = max(longest, run)
            prev = d
        }
        return longest
    }

    private companion object {
        const val MATURE_THRESHOLD_DAYS = 21
    }
}
