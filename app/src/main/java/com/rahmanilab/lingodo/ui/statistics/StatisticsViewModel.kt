package com.rahmanilab.lingodo.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rahmanilab.lingodo.data.repository.StatsRepository
import com.rahmanilab.lingodo.domain.model.Statistics
import com.rahmanilab.lingodo.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatisticsUiState(
    val stats: Statistics = Statistics(),
    val loading: Boolean = true
)

class StatisticsViewModel(private val statsRepository: StatsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val stats = statsRepository.computeStatistics(days = 30)
            _uiState.value = StatisticsUiState(stats = stats, loading = false)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { StatisticsViewModel(appContainer.statsRepository) }
        }
    }
}
