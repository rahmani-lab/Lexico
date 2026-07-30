package com.rahmanilab.lingodo.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rahmanilab.lingodo.data.net.AiErrors
import com.rahmanilab.lingodo.data.practice.PracticeRepository
import com.rahmanilab.lingodo.data.practice.PracticeStyleRepository
import com.rahmanilab.lingodo.domain.practice.PracticeExercise
import com.rahmanilab.lingodo.domain.practice.PracticeStyle
import com.rahmanilab.lingodo.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SmartPracticeUiState(
    val summary: String = "",
    val report: String = "",
    val styles: List<PracticeStyle> = PracticeStyle.presets,
    val selectedStyleId: String = PracticeStyle.DEFAULT_ID,
    val loadingContext: Boolean = true,
    val busy: Boolean = false,
    val exercises: List<PracticeExercise> = emptyList(),
    val currentIndex: Int = 0,
    val typedAnswer: String = "",
    val checked: Boolean = false,
    val lastCorrect: Boolean? = null,
    val correctCount: Int = 0,
    val finished: Boolean = false,
    val error: String? = null
) {
    val current: PracticeExercise? get() = exercises.getOrNull(currentIndex)
    val started: Boolean get() = exercises.isNotEmpty()
}

class SmartPracticeViewModel(
    private val practice: PracticeRepository,
    private val styleRepository: PracticeStyleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmartPracticeUiState())
    val uiState: StateFlow<SmartPracticeUiState> = _uiState.asStateFlow()

    init {
        loadContext()
        observeStyles()
    }

    private fun observeStyles() {
        viewModelScope.launch {
            combine(styleRepository.styles, styleRepository.activeStyleId) { styles, active ->
                styles to active
            }.collect { (styles, active) ->
                _uiState.update { it.copy(styles = styles, selectedStyleId = active) }
            }
        }
    }

    fun setStyle(id: String) {
        _uiState.update { it.copy(selectedStyleId = id) }
        viewModelScope.launch { styleRepository.setActive(id) }
    }

    fun loadContext() {
        viewModelScope.launch {
            _uiState.update { it.copy(loadingContext = true) }
            val ctx = runCatching { practice.buildContext() }.getOrNull()
            _uiState.update { it.copy(summary = ctx?.summary.orEmpty(), loadingContext = false) }
        }
    }

    fun generateReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null) }
            practice.generateReport()
                .onSuccess { report -> _uiState.update { it.copy(report = report, busy = false) } }
                .onFailure { e -> _uiState.update { it.copy(error = AiErrors.friendlyMessage(e), busy = false) } }
        }
    }

    fun startExercises() {
        val state = _uiState.value
        val style = state.styles.firstOrNull { it.id == state.selectedStyleId } ?: PracticeStyle.default()
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null) }
            practice.generateExercises(style.instructions)
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(
                            exercises = list,
                            currentIndex = 0,
                            typedAnswer = "",
                            checked = false,
                            lastCorrect = null,
                            correctCount = 0,
                            finished = false,
                            busy = false
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(error = AiErrors.friendlyMessage(e), busy = false) } }
        }
    }

    fun setTyped(value: String) = _uiState.update { it.copy(typedAnswer = value) }

    fun check() {
        val state = _uiState.value
        val exercise = state.current ?: return
        if (state.checked) return
        viewModelScope.launch {
            val correct = practice.submitAnswer(exercise, state.typedAnswer)
            _uiState.update {
                it.copy(
                    checked = true,
                    lastCorrect = correct,
                    correctCount = it.correctCount + if (correct) 1 else 0
                )
            }
        }
    }

    fun next() {
        _uiState.update {
            val nextIndex = it.currentIndex + 1
            if (nextIndex >= it.exercises.size) {
                it.copy(finished = true)
            } else {
                it.copy(currentIndex = nextIndex, typedAnswer = "", checked = false, lastCorrect = null)
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                SmartPracticeViewModel(
                    appContainer.practiceRepository,
                    appContainer.practiceStyleRepository
                )
            }
        }
    }
}
