package com.rahmanilab.lingodo.ui.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rahmanilab.lingodo.data.local.entity.LanguagePairEntity
import com.rahmanilab.lingodo.data.repository.LanguagePairRepository
import com.rahmanilab.lingodo.domain.model.Language
import com.rahmanilab.lingodo.ui.appContainer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkspaceUiState(
    val pairs: List<LanguagePairEntity> = emptyList(),
    val activePairId: Long = 1L
)

class WorkspaceViewModel(
    private val repository: LanguagePairRepository
) : ViewModel() {

    val uiState: StateFlow<WorkspaceUiState> =
        combine(repository.observePairs(), repository.activePairId) { pairs, active ->
            WorkspaceUiState(pairs = pairs, activePairId = active)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkspaceUiState())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun setActive(id: Long) = viewModelScope.launch { repository.setActive(id) }

    fun createPair(source: Language, target: Language) = viewModelScope.launch {
        if (source == target) {
            _messages.tryEmit("Source and target languages must differ.")
            return@launch
        }
        val id = repository.createPair(source.code, target.code)
        repository.setActive(id)
    }

    fun deletePair(pair: LanguagePairEntity) = viewModelScope.launch {
        repository.deletePair(pair)?.let { _messages.tryEmit(it) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { WorkspaceViewModel(appContainer.languagePairRepository) }
        }
    }
}
