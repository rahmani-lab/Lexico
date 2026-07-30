package com.rahmanilab.lingodo.data.repository

import androidx.room.withTransaction
import com.rahmanilab.lingodo.data.local.LingoDoDatabase
import com.rahmanilab.lingodo.data.local.entity.CardScheduleEntity
import com.rahmanilab.lingodo.data.local.entity.ReviewLogEntity
import com.rahmanilab.lingodo.data.local.relation.CardWithDetails
import com.rahmanilab.lingodo.data.preferences.SettingsRepository
import com.rahmanilab.lingodo.data.preferences.model.SchedulerType
import com.rahmanilab.lingodo.domain.model.CardState
import com.rahmanilab.lingodo.domain.model.Rating
import com.rahmanilab.lingodo.domain.scheduler.Scheduler
import com.rahmanilab.lingodo.domain.scheduler.SchedulingState
import com.rahmanilab.lingodo.util.DateUtils

/** The cards selected for a study session, split so the UI can show "new vs due" counts. */
data class ReviewQueue(
    val cards: List<CardWithDetails>,
    val dueCount: Int,
    val newCount: Int
) {
    val total: Int get() = cards.size
    val isEmpty: Boolean get() = cards.isEmpty()
}

class ReviewRepository(
    private val db: LingoDoDatabase,
    private val sm2Scheduler: Scheduler,
    private val fsrsScheduler: Scheduler,
    private val settingsRepository: SettingsRepository
) {

    private val cardDao get() = db.cardDao()
    private val scheduleDao get() = db.cardScheduleDao()
    private val reviewLogDao get() = db.reviewLogDao()
    private val deckDao get() = db.deckDao()

    /** The scheduler chosen in Settings, resolved when a session's queue is built. */
    @Volatile
    private var activeScheduler: Scheduler = fsrsScheduler

    private suspend fun resolveActiveScheduler() {
        activeScheduler = when (settingsRepository.current().schedulerType) {
            SchedulerType.FSRS -> fsrsScheduler
            SchedulerType.SM2 -> sm2Scheduler
        }
    }

    /**
     * Build a study queue: every due card plus up to the remaining daily allowance of new cards.
     *
     * @param deckId restrict to one deck, or null for all decks
     * @param includeNew whether to introduce new cards at all
     * @param respectDailyLimit when false, the daily new-card cap is ignored (e.g. "cram" mode)
     */
    suspend fun buildQueue(
        deckId: Long?,
        includeNew: Boolean = true,
        respectDailyLimit: Boolean = true
    ): ReviewQueue {
        val now = System.currentTimeMillis()
        val settings = settingsRepository.current()
        resolveActiveScheduler()

        // Restrict the session to the active language pair's decks. A parent deck ("book") expands
        // to itself plus all of its lessons, so "Study all" reviews the whole book in one session.
        val pairId = settingsRepository.currentActivePairId()
        val deckIds = deckId?.let { listOf(it) + deckDao.childDeckIds(it) }
        val due = if (deckIds == null) cardDao.getDueCards(now, null, pairId)
        else cardDao.getDueCardsIn(now, deckIds, pairId)

        val newCards = if (includeNew) {
            val limit = if (respectDailyLimit) {
                val studied = settingsRepository.newCardsStudiedToday(DateUtils.todayEpochDay())
                (settings.dailyNewLimit - studied).coerceAtLeast(0)
            } else {
                Int.MAX_VALUE
            }
            if (limit > 0) {
                if (deckIds == null) cardDao.getNewCards(null, limit, pairId)
                else cardDao.getNewCardsIn(deckIds, limit, pairId)
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }

        // Due cards first (already ordered by due time), then new cards.
        return ReviewQueue(cards = due + newCards, dueCount = due.size, newCount = newCards.size)
    }

    /** Preview the interval each rating would produce, for labelling the four answer buttons. */
    fun preview(card: CardWithDetails, now: Long = System.currentTimeMillis()): Map<Rating, SchedulingState> {
        val existing = card.schedule ?: initialSchedule(card.card.id, now)
        return activeScheduler.preview(existing.toSchedulingState(), now)
    }

    /** Record an answer: advance the schedule, append an immutable review log, and count new cards. */
    suspend fun answer(
        card: CardWithDetails,
        rating: Rating,
        responseTimeMillis: Long? = null,
        now: Long = System.currentTimeMillis()
    ): CardScheduleEntity {
        // Ensure the configured scheduler is active even when answering outside a review session
        // (e.g. from Smart Practice).
        resolveActiveScheduler()
        val existing = card.schedule ?: initialSchedule(card.card.id, now)
        val wasNew = CardState.fromString(existing.state) == CardState.NEW
        val currentState = existing.toSchedulingState()
        val next = activeScheduler.schedule(currentState, rating, now)
        val nextEntity = next.toEntity(card.card.id, existing)

        db.withTransaction {
            scheduleDao.upsert(nextEntity)
            reviewLogDao.insert(
                ReviewLogEntity(
                    cardId = card.card.id,
                    rating = rating.value,
                    reviewedAt = now,
                    previousIntervalDays = currentState.intervalDays,
                    nextIntervalDays = next.intervalDays,
                    responseTimeMillis = responseTimeMillis
                )
            )
        }

        if (wasNew) {
            settingsRepository.recordNewCardsStudied(1, DateUtils.todayEpochDay())
        }
        return nextEntity
    }
}
