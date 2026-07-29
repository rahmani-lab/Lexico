package com.rahmanilab.lingodo.domain.scheduler

import com.rahmanilab.lingodo.domain.model.CardState
import com.rahmanilab.lingodo.domain.model.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Sm2SchedulerTest {

    private val scheduler = Sm2Scheduler()
    private val config = SchedulerConfig()

    private fun minutes(m: Int) = m * 60_000L
    private fun days(d: Int) = d * 86_400_000L

    private fun newCard() = SchedulingState.newCard(now = 0L)

    private fun reviewCard(intervalDays: Int, ease: Double = 2.5) = SchedulingState(
        state = CardState.REVIEW,
        dueAt = 0L,
        intervalDays = intervalDays,
        easeFactor = ease,
        repetitions = 3,
        lapses = 0,
        learningStepIndex = 0,
        lastReviewedAt = 0L
    )

    @Test
    fun newCard_again_staysInLearningAtFirstStep() {
        val result = scheduler.schedule(newCard(), Rating.AGAIN, now = 0L)
        assertEquals(CardState.LEARNING, result.state)
        assertEquals(0, result.learningStepIndex)
        assertEquals(minutes(1), result.dueAt)
    }

    @Test
    fun newCard_good_advancesToNextLearningStep() {
        val result = scheduler.schedule(newCard(), Rating.GOOD, now = 0L)
        assertEquals(CardState.LEARNING, result.state)
        assertEquals(1, result.learningStepIndex)
        assertEquals(minutes(10), result.dueAt)
    }

    @Test
    fun newCard_easy_graduatesImmediately() {
        val result = scheduler.schedule(newCard(), Rating.EASY, now = 0L)
        assertEquals(CardState.REVIEW, result.state)
        assertEquals(config.easyIntervalDays, result.intervalDays)
        assertEquals(days(config.easyIntervalDays), result.dueAt)
    }

    @Test
    fun learning_good_onLastStep_graduatesToReview() {
        val onLastStep = newCard().copy(state = CardState.LEARNING, learningStepIndex = 1)
        val result = scheduler.schedule(onLastStep, Rating.GOOD, now = 0L)
        assertEquals(CardState.REVIEW, result.state)
        assertEquals(config.graduatingIntervalDays, result.intervalDays)
    }

    @Test
    fun review_good_multipliesIntervalByEase() {
        val result = scheduler.schedule(reviewCard(intervalDays = 10, ease = 2.5), Rating.GOOD, now = 0L)
        assertEquals(CardState.REVIEW, result.state)
        assertEquals(25, result.intervalDays) // 10 * 2.5
        assertEquals(days(25), result.dueAt)
    }

    @Test
    fun review_hard_growsSlowlyAndLowersEase() {
        val result = scheduler.schedule(reviewCard(intervalDays = 10, ease = 2.5), Rating.HARD, now = 0L)
        assertEquals(12, result.intervalDays) // round(10 * 1.2)
        assertEquals(2.35, result.easeFactor, 1e-9)
    }

    @Test
    fun review_easy_growsFasterAndRaisesEase() {
        val result = scheduler.schedule(reviewCard(intervalDays = 10, ease = 2.5), Rating.EASY, now = 0L)
        assertEquals(33, result.intervalDays) // round(10 * 2.5 * 1.3)
        assertEquals(2.65, result.easeFactor, 1e-9)
    }

    @Test
    fun review_again_lapsesIntoRelearning() {
        val result = scheduler.schedule(reviewCard(intervalDays = 10, ease = 2.5), Rating.AGAIN, now = 0L)
        assertEquals(CardState.RELEARNING, result.state)
        assertEquals(1, result.lapses)
        assertEquals(2.30, result.easeFactor, 1e-9)
        assertEquals(5, result.intervalDays) // round(10 * 0.5) kept for re-graduation
        assertEquals(minutes(10), result.dueAt)
    }

    @Test
    fun relearning_good_graduatesBackToReviewWithReducedInterval() {
        val relearning = reviewCard(intervalDays = 5).copy(
            state = CardState.RELEARNING,
            learningStepIndex = 0
        )
        val result = scheduler.schedule(relearning, Rating.GOOD, now = 0L)
        assertEquals(CardState.REVIEW, result.state)
        assertEquals(5, result.intervalDays)
        assertEquals(days(5), result.dueAt)
    }

    @Test
    fun ease_neverDropsBelowMinimum() {
        val nearFloor = reviewCard(intervalDays = 10, ease = config.minEase)
        val result = scheduler.schedule(nearFloor, Rating.AGAIN, now = 0L)
        assertTrue(result.easeFactor >= config.minEase)
    }

    @Test
    fun interval_alwaysGrowsByAtLeastOneDay() {
        // With a low ease, GOOD on a 1-day interval must still move forward.
        val result = scheduler.schedule(reviewCard(intervalDays = 1, ease = config.minEase), Rating.GOOD, now = 0L)
        assertTrue(result.intervalDays > 1)
    }

    @Test
    fun preview_returnsAnOutcomeForEveryRating() {
        val preview = scheduler.preview(newCard(), now = 0L)
        assertEquals(Rating.entries.size, preview.size)
        Rating.entries.forEach { assertTrue(preview.containsKey(it)) }
    }
}
