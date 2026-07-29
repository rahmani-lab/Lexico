package com.rahmanilab.lingodo.ui.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rahmanilab.lingodo.data.local.relation.CardWithDetails
import com.rahmanilab.lingodo.data.preferences.SettingsRepository
import com.rahmanilab.lingodo.data.repository.ReviewRepository
import com.rahmanilab.lingodo.domain.model.Rating
import com.rahmanilab.lingodo.domain.model.ReviewMode
import com.rahmanilab.lingodo.ui.appContainer
import com.rahmanilab.lingodo.ui.navigation.Routes
import com.rahmanilab.lingodo.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionSummary(
    val reviewed: Int,
    val again: Int,
    val hard: Int,
    val good: Int,
    val easy: Int,
    val durationMillis: Long
) {
    val correct: Int get() = hard + good + easy
    val accuracyPercent: Int get() = if (reviewed > 0) (correct * 100) / reviewed else 0
}

data class ReviewUiState(
    val loading: Boolean = true,
    val finished: Boolean = false,
    val mode: ReviewMode = ReviewMode.FRONT_TO_BACK,
    val current: CardWithDetails? = null,
    val isRevealed: Boolean = false,
    val intervals: Map<Rating, String> = emptyMap(),
    val remaining: Int = 0,
    val reviewedCount: Int = 0,
    val newCount: Int = 0,
    val dueCount: Int = 0,
    val typedAnswer: String = "",
    val answerChecked: Boolean = false,
    val answerCorrect: Boolean? = null,
    val showPhonetic: Boolean = true,
    val autoPlay: Boolean = true,
    val summary: SessionSummary? = null
)

class ReviewViewModel(
    private val reviewRepository: ReviewRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val deckId: Long? = savedStateHandle.get<Long>(Routes.ARG_DECK_ID)?.takeIf { it > 0 }

    private val queue = ArrayDeque<CardWithDetails>()
    private var sessionStart = System.currentTimeMillis()
    private var sessionLimitMillis = 0L
    private var cardShownAt = System.currentTimeMillis()

    private var again = 0
    private var hard = 0
    private var good = 0
    private var easy = 0
    private var reviewed = 0

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        start()
    }

    fun start() {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            sessionLimitMillis = settings.sessionLengthMinutes * 60_000L
            sessionStart = System.currentTimeMillis()
            again = 0; hard = 0; good = 0; easy = 0; reviewed = 0
            queue.clear()

            val q = reviewRepository.buildQueue(deckId, includeNew = true, respectDailyLimit = true)
            queue.addAll(q.cards)

            _uiState.update {
                it.copy(
                    loading = false,
                    finished = false,
                    newCount = q.newCount,
                    dueCount = q.dueCount,
                    reviewedCount = 0,
                    summary = null,
                    showPhonetic = settings.showPhonetic,
                    autoPlay = settings.autoPlayPronunciation
                )
            }
            showNext()
        }
    }

    private fun showNext() {
        val next = queue.firstOrNull()
        if (next == null) {
            finish()
            return
        }
        cardShownAt = System.currentTimeMillis()
        val intervals = reviewRepository.preview(next).mapValues { (_, s) ->
            DateUtils.formatInterval(s.dueAt - System.currentTimeMillis())
        }
        _uiState.update {
            it.copy(
                current = next,
                isRevealed = false,
                typedAnswer = "",
                answerChecked = false,
                answerCorrect = null,
                intervals = intervals,
                remaining = queue.size
            )
        }
    }

    fun reveal() {
        _uiState.update { it.copy(isRevealed = true) }
    }

    fun setMode(mode: ReviewMode) {
        _uiState.update {
            it.copy(
                mode = mode,
                isRevealed = false,
                typedAnswer = "",
                answerChecked = false,
                answerCorrect = null
            )
        }
    }

    fun setTypedAnswer(value: String) {
        _uiState.update { it.copy(typedAnswer = value) }
    }

    fun checkTypedAnswer() {
        val card = _uiState.value.current?.card ?: return
        val correct = com.rahmanilab.lingodo.util.TextUtils.answersMatch(_uiState.value.typedAnswer, card.word)
        _uiState.update { it.copy(answerChecked = true, answerCorrect = correct, isRevealed = true) }
    }

    fun rate(rating: Rating) {
        val current = _uiState.value.current ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val responseTime = now - cardShownAt
            val updated = reviewRepository.answer(current, rating, responseTimeMillis = responseTime, now = now)

            reviewed++
            when (rating) {
                Rating.AGAIN -> again++
                Rating.HARD -> hard++
                Rating.GOOD -> good++
                Rating.EASY -> easy++
            }

            queue.removeFirstOrNull()
            // Re-show the card later this session if it's still in a short (re)learning step.
            if ((updated.state == "LEARNING" || updated.state == "RELEARNING") &&
                updated.dueAt <= now + REQUEUE_WINDOW_MS
            ) {
                queue.addLast(current.copy(schedule = updated))
            }

            _uiState.update { it.copy(reviewedCount = reviewed) }

            if (sessionLimitMillis > 0 && (now - sessionStart) >= sessionLimitMillis) {
                finish()
            } else {
                showNext()
            }
        }
    }

    private fun finish() {
        _uiState.update {
            it.copy(
                finished = true,
                current = null,
                summary = SessionSummary(
                    reviewed = reviewed,
                    again = again,
                    hard = hard,
                    good = good,
                    easy = easy,
                    durationMillis = System.currentTimeMillis() - sessionStart
                )
            )
        }
    }

    companion object {
        private const val REQUEUE_WINDOW_MS = 20 * 60 * 1000L

        val Factory = viewModelFactory {
            initializer {
                ReviewViewModel(
                    appContainer.reviewRepository,
                    appContainer.settingsRepository,
                    createSavedStateHandle()
                )
            }
        }
    }
}
