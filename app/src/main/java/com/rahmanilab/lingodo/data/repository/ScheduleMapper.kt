package com.rahmanilab.lingodo.data.repository

import com.rahmanilab.lingodo.data.local.entity.CardScheduleEntity
import com.rahmanilab.lingodo.domain.model.CardState
import com.rahmanilab.lingodo.domain.scheduler.SchedulingState

/** Bridges the persistent [CardScheduleEntity] and the pure-domain [SchedulingState]. */

fun CardScheduleEntity.toSchedulingState(): SchedulingState = SchedulingState(
    state = CardState.fromString(state),
    dueAt = dueAt,
    intervalDays = intervalDays,
    easeFactor = easeFactor,
    repetitions = repetitions,
    lapses = lapses,
    learningStepIndex = learningStepIndex,
    lastReviewedAt = lastReviewedAt,
    difficulty = difficulty,
    stability = stability
)

fun SchedulingState.toEntity(cardId: Long, existing: CardScheduleEntity? = null): CardScheduleEntity =
    CardScheduleEntity(
        cardId = cardId,
        state = state.name,
        dueAt = dueAt,
        repetitions = repetitions,
        lapses = lapses,
        intervalDays = intervalDays,
        easeFactor = easeFactor,
        learningStepIndex = learningStepIndex,
        lastReviewedAt = lastReviewedAt,
        // Carry FSRS memory state through; SM-2 leaves whatever was already there untouched.
        difficulty = difficulty ?: existing?.difficulty,
        stability = stability ?: existing?.stability
    )

fun initialSchedule(cardId: Long, now: Long): CardScheduleEntity =
    CardScheduleEntity(cardId = cardId, state = CardState.NEW.name, dueAt = now)
