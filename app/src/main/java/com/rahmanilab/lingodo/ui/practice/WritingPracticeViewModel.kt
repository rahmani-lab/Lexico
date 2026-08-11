package com.rahmanilab.lingodo.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rahmanilab.lingodo.data.net.AiErrors
import com.rahmanilab.lingodo.data.practice.WritingRepository
import com.rahmanilab.lingodo.domain.practice.TipHighlight
import com.rahmanilab.lingodo.domain.practice.WRITING_WORD_LIMIT
import com.rahmanilab.lingodo.domain.practice.WritingAnalysis
import com.rahmanilab.lingodo.domain.practice.WritingTopic
import com.rahmanilab.lingodo.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WritingPracticeUiState(
    val topic: WritingTopic = WritingTopic(prompt = ""),
    val essay: String = "",
    val loadingTopic: Boolean = true,
    val checking: Boolean = false,
    val analysis: WritingAnalysis? = null,
    val highlights: List<TipHighlight> = emptyList(),
    /** Tip the carousel should jump to (set by tapping a highlight); cleared once consumed. */
    val focusTipIndex: Int? = null,
    val rewardedWords: List<String> = emptyList(),
    val error: String? = null
) {
    val wordCount: Int get() = essay.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    val overLimit: Boolean get() = wordCount > WRITING_WORD_LIMIT
    val canCheck: Boolean get() = essay.isNotBlank() && !overLimit && !checking
    val hasResult: Boolean get() = analysis != null
}

class WritingPracticeViewModel(
    private val writing: WritingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WritingPracticeUiState())
    val uiState: StateFlow<WritingPracticeUiState> = _uiState.asStateFlow()

    init {
        loadTopic()
    }

    fun loadTopic() {
        viewModelScope.launch {
            _uiState.update { it.copy(loadingTopic = true) }
            val topic = runCatching { writing.buildTopic() }
                .getOrElse { WritingTopic(prompt = "Write a few sentences about your day.") }
            _uiState.update { it.copy(topic = topic, loadingTopic = false) }
        }
    }

    fun setEssay(value: String) = _uiState.update { it.copy(essay = value) }

    fun check() {
        val state = _uiState.value
        if (!state.canCheck) return
        viewModelScope.launch {
            _uiState.update { it.copy(checking = true, error = null) }
            writing.analyze(state.topic, state.essay)
                .onSuccess { analysis ->
                    // Reward target words the coach didn't flag, closing the SRS loop.
                    val rewarded = runCatching {
                        writing.rewardCorrectlyUsedWords(state.topic, state.essay, analysis)
                    }.getOrDefault(emptyList())
                    _uiState.update {
                        it.copy(
                            analysis = analysis,
                            highlights = writing.highlightsFor(it.essay, analysis),
                            rewardedWords = rewarded,
                            checking = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = AiErrors.friendlyMessage(e), checking = false) }
                }
        }
    }

    /** Tapping a highlighted phrase jumps the carousel to its tip. */
    fun focusTip(index: Int) = _uiState.update { it.copy(focusTipIndex = index) }

    fun consumeFocus() = _uiState.update { it.copy(focusTipIndex = null) }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    /** Back to a blank page with a fresh topic. */
    fun startOver() {
        _uiState.update {
            it.copy(
                essay = "",
                analysis = null,
                highlights = emptyList(),
                focusTipIndex = null,
                rewardedWords = emptyList(),
                error = null
            )
        }
        loadTopic()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { WritingPracticeViewModel(appContainer.writingRepository) }
        }
    }
}
