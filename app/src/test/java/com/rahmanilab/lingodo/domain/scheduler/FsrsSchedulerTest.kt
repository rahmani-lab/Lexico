package com.rahmanilab.lingodo.domain.scheduler

import com.rahmanilab.lingodo.domain.model.CardState
import com.rahmanilab.lingodo.domain.model.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FsrsSchedulerTest {

    private val scheduler = FsrsScheduler()
    private val day = 86_400_000L

    private fun newCard() = SchedulingState.newCard(now = 0L)

    private fun reviewCard(stability: Double, difficulty: Double, elapsedDays: Long) = SchedulingState(
        state = CardState.REVIEW,
        dueAt = 0L,
        intervalDays = stability.toInt(),
        easeFactor = 2.5,
        repetitions = 5,
        lapses = 0,
        learningStepIndex = 0,
        lastReviewedAt = 0L,
        difficulty = difficulty,
        stability = stability
    ).let { it to elapsedDays * day }

    @Test
    fun newCard_good_initialisesMemoryStateAndGraduates() {
        val result = scheduler.schedule(newCard(), Rating.GOOD, now = 0L)
        assertEquals(CardState.REVIEW, result.state)
        assertNotNull(result.stability)
        assertNotNull(result.difficulty)
        assertTrue(result.intervalDays >= 1)
        assertEquals(result.intervalDays * day, result.dueAt)
    }

    @Test
    fun newCard_again_entersRelearningWithShortStep() {
        val result = scheduler.schedule(newCard(), Rating.AGAIN, now = 0L)
        assertEquals(CardState.RELEARNING, result.state)
        assertEquals(10 * 60_000L, result.dueAt)
        assertNotNull(result.stability)
    }

    @Test
    fun difficultyAlwaysStaysWithinBounds() {
        Rating.entries.forEach { rating ->
            val result = scheduler.schedule(newCard(), rating, now = 0L)
            val difficulty = result.difficulty!!
            assertTrue(difficulty in 1.0..10.0)
        }
    }

    @Test
    fun review_intervalsAreMonotonicByGrade() {
        val (card, now) = reviewCard(stability = 10.0, difficulty = 5.0, elapsedDays = 10)
        val hard = scheduler.schedule(card, Rating.HARD, now).intervalDays
        val good = scheduler.schedule(card, Rating.GOOD, now).intervalDays
        val easy = scheduler.schedule(card, Rating.EASY, now).intervalDays
        assertTrue("easy >= good", easy >= good)
        assertTrue("good >= hard", good >= hard)
        assertTrue(hard >= 1)
    }

    @Test
    fun review_again_countsLapseAndReducesStability() {
        val (card, now) = reviewCard(stability = 20.0, difficulty = 5.0, elapsedDays = 20)
        val result = scheduler.schedule(card, Rating.AGAIN, now)
        assertEquals(CardState.RELEARNING, result.state)
        assertEquals(1, result.lapses)
        assertTrue("post-lapse stability must not exceed prior", result.stability!! <= 20.0)
    }

    @Test
    fun preview_coversEveryRating() {
        val preview = scheduler.preview(newCard(), now = 0L)
        assertEquals(Rating.entries.size, preview.size)
    }
}
