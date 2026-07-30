package com.rahmanilab.lingodo.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rahmanilab.lingodo.data.backup.ImportExportRepository
import com.rahmanilab.lingodo.data.practice.PracticeStyleRepository
import com.rahmanilab.lingodo.data.preferences.SettingsRepository
import com.rahmanilab.lingodo.data.preferences.model.AppSettings
import com.rahmanilab.lingodo.data.preferences.model.SchedulerType
import com.rahmanilab.lingodo.data.preferences.model.ThemeMode
import com.rahmanilab.lingodo.data.preferences.model.TtsAccent
import com.rahmanilab.lingodo.data.repository.AiConfigRepository
import com.rahmanilab.lingodo.data.repository.LanguagePairRepository
import com.rahmanilab.lingodo.domain.model.AiProvider
import com.rahmanilab.lingodo.domain.model.Language
import com.rahmanilab.lingodo.domain.practice.PracticeStyle
import com.rahmanilab.lingodo.tts.PronunciationManager
import com.rahmanilab.lingodo.ui.appContainer
import com.rahmanilab.lingodo.work.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
    private val pronunciationManager: PronunciationManager,
    private val importExportRepository: ImportExportRepository,
    private val aiConfigRepository: AiConfigRepository,
    private val practiceStyleRepository: PracticeStyleRepository,
    private val languagePairRepository: LanguagePairRepository
) : ViewModel() {

    /** The active pair's target language — drives the TTS locale, voice list and voice test. */
    val activeTargetLanguage: StateFlow<Language> = languagePairRepository.activePairId
        .map { languagePairRepository.activePair().target }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Language.ENGLISH)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val aiProvider: StateFlow<AiProvider> = aiConfigRepository.provider
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiProvider.GEMINI)

    /** Whether the currently selected provider has a saved API key. */
    val hasApiKey: StateFlow<Boolean> = aiConfigRepository.provider
        .map { aiConfigRepository.hasKey(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val practiceStyles: StateFlow<List<PracticeStyle>> = practiceStyleRepository.styles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PracticeStyle.presets)

    val activePracticeStyleId: StateFlow<String> = practiceStyleRepository.activeStyleId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PracticeStyle.DEFAULT_ID)

    fun setActivePracticeStyle(id: String) = launch { practiceStyleRepository.setActive(id) }
    fun savePracticeStyle(style: PracticeStyle) = launch { practiceStyleRepository.saveCustom(style) }
    fun deletePracticeStyle(id: String) = launch { practiceStyleRepository.deleteCustom(id) }

    /** Installed voices for the active target language, for the Settings voice picker. */
    val voices: StateFlow<List<String>> =
        combine(pronunciationManager.isReady, activeTargetLanguage) { ready, lang -> ready to lang }
            .filter { it.first }
            .map { pronunciationManager.availableVoices(it.second.ttsTag) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTheme(mode: ThemeMode) = launch { settingsRepository.setThemeMode(mode) }
    fun setAccent(accent: TtsAccent) = launch { settingsRepository.setAccent(accent) }
    fun setSpeechRate(rate: Float) = launch { settingsRepository.setSpeechRate(rate) }
    fun setVoice(name: String?) = launch { settingsRepository.setSelectedVoice(name) }
    fun setAutoPlay(enabled: Boolean) = launch { settingsRepository.setAutoPlay(enabled) }
    fun setShowPhonetic(show: Boolean) = launch { settingsRepository.setShowPhonetic(show) }
    fun setDailyNewLimit(limit: Int) = launch { settingsRepository.setDailyNewLimit(limit) }
    fun setSessionLength(minutes: Int) = launch { settingsRepository.setSessionLength(minutes) }
    fun setScheduler(type: SchedulerType) = launch { settingsRepository.setSchedulerType(type) }

    // --- AI auto-fill (BYOK) ---

    fun setAiProvider(provider: AiProvider) = launch { aiConfigRepository.setProvider(provider) }

    fun saveApiKey(key: String) = launch {
        if (key.isBlank()) return@launch
        aiConfigRepository.setKey(aiConfigRepository.currentProvider(), key)
        _messages.tryEmit("API key saved securely on this device.")
    }

    fun clearApiKey() = launch {
        aiConfigRepository.clearKey(aiConfigRepository.currentProvider())
        _messages.tryEmit("API key removed.")
    }

    fun setReminderEnabled(enabled: Boolean) = launch {
        settingsRepository.setReminderEnabled(enabled)
        applyReminder()
    }

    fun setReminderTime(hour: Int, minute: Int) = launch {
        settingsRepository.setReminderTime(hour, minute)
        applyReminder()
    }

    /** Preview the current voice/rate with a sample word in the active target language. */
    fun testVoice() {
        val current = settings.value
        val target = activeTargetLanguage.value
        val locale = PronunciationManager.resolveTtsLocale(target.code, current.ttsAccent)
        pronunciationManager.speak(target.sampleWord, locale, current.selectedVoice, current.speechRate)
    }

    // ------------------------------------------------------------ data import/export

    fun exportCards(resolver: ContentResolver, uri: Uri, asJson: Boolean) = viewModelScope.launch {
        runCatching {
            val text = if (asJson) importExportRepository.exportCardsJson()
            else importExportRepository.exportCardsCsv()
            writeText(resolver, uri, text)
        }.onSuccess { _messages.tryEmit("Cards exported.") }
            .onFailure { _messages.tryEmit("Export failed: ${it.message}") }
    }

    fun exportBackup(resolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        runCatching { writeText(resolver, uri, importExportRepository.exportBackupJson()) }
            .onSuccess { _messages.tryEmit("Backup saved.") }
            .onFailure { _messages.tryEmit("Backup failed: ${it.message}") }
    }

    fun importCards(resolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        runCatching {
            val text = readText(resolver, uri)
            val trimmed = text.trimStart()
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                importExportRepository.importJson(text)
            } else {
                importExportRepository.importCsv(text)
            }
        }.onSuccess { _messages.tryEmit(it.message) }
            .onFailure { _messages.tryEmit("Import failed: ${it.message}") }
    }

    fun importAnki(resolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        runCatching { importExportRepository.importAnkiTsv(readText(resolver, uri)) }
            .onSuccess { _messages.tryEmit(it.message) }
            .onFailure { _messages.tryEmit("Anki import failed: ${it.message}") }
    }

    fun restoreBackup(resolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        runCatching { importExportRepository.restoreBackupJson(readText(resolver, uri)) }
            .onSuccess { _messages.tryEmit(it.message) }
            .onFailure { _messages.tryEmit("Restore failed: ${it.message}") }
    }

    private suspend fun writeText(resolver: ContentResolver, uri: Uri, text: String) =
        withContext(Dispatchers.IO) {
            resolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
                ?: error("Could not open file for writing")
        }

    private suspend fun readText(resolver: ContentResolver, uri: Uri): String =
        withContext(Dispatchers.IO) {
            resolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                ?: error("Could not open file for reading")
        }

    private suspend fun applyReminder() {
        val current = settingsRepository.current()
        if (current.reminderEnabled) {
            reminderScheduler.schedule(current.reminderHour, current.reminderMinute)
        } else {
            reminderScheduler.cancel()
        }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    appContainer.settingsRepository,
                    appContainer.reminderScheduler,
                    appContainer.pronunciationManager,
                    appContainer.importExportRepository,
                    appContainer.aiConfigRepository,
                    appContainer.practiceStyleRepository,
                    appContainer.languagePairRepository
                )
            }
        }
    }
}
