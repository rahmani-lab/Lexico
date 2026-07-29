package com.rahmanilab.lingodo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rahmanilab.lingodo.data.preferences.SettingsRepository
import com.rahmanilab.lingodo.data.repository.StatsRepository
import com.rahmanilab.lingodo.ui.appContainer
import com.rahmanilab.lingodo.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val dueCount: Int = 0,
    val newCount: Int = 0,
    val reviewsToday: Int = 0,
    val streak: Int = 0,
    val hasCards: Boolean = false,
    val loading: Boolean = true
)

class HomeViewModel(
    private val stats: StatsRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val today = DateUtils.todayEpochDay()

    private data class Counts(val due: Int, val newAvailable: Int, val reviews: Int, val total: Int)

    // Counts follow the active language pair; reviews-today stays global.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val countsFlow = settings.activePairId.flatMapLatest { pairId ->
        combine(
            stats.observeDueCountForPair(pairId),
            stats.observeNewCountForPair(pairId),
            stats.observeReviewsToday(),
            stats.observeTotalCardsForPair(pairId)
        ) { due, newAvailable, reviews, total -> Counts(due, newAvailable, reviews, total) }
    }

    private val remainingNewFlow = combine(
        settings.settings,
        settings.newCardsStudiedTodayFlow(today)
    ) { appSettings, studied -> (appSettings.dailyNewLimit - studied).coerceAtLeast(0) }

    /** Streak is a heavier computation, refreshed on load and whenever the screen resumes. */
    private val streakFlow = MutableStateFlow(0)

    val uiState: StateFlow<HomeUiState> =
        combine(countsFlow, remainingNewFlow, streakFlow) { counts, remainingNew, streak ->
            HomeUiState(
                dueCount = counts.due,
                newCount = minOf(counts.newAvailable, remainingNew),
                reviewsToday = counts.reviews,
                streak = streak,
                hasCards = counts.total > 0,
                loading = false
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        refreshStreak()
    }

    fun refreshStreak() {
        viewModelScope.launch {
            streakFlow.value = stats.computeStatistics().currentStreak
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                HomeViewModel(appContainer.statsRepository, appContainer.settingsRepository)
            }
        }
    }
}
