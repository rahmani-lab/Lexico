package com.rahmanilab.lingodo.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rahmanilab.lingodo.data.preferences.model.AppSettings
import com.rahmanilab.lingodo.data.preferences.model.SchedulerType
import com.rahmanilab.lingodo.data.preferences.model.ThemeMode
import com.rahmanilab.lingodo.data.preferences.model.TtsAccent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * DataStore-backed store for [AppSettings] plus a couple of internal bookkeeping values (the
 * one-time seed flag and the per-day "new cards studied" counter used to enforce the daily limit).
 */
class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toAppSettings() }

    suspend fun current(): AppSettings = context.dataStore.data.first().toAppSettings()

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME] = mode.name }
    suspend fun setAccent(accent: TtsAccent) = edit { it[Keys.ACCENT] = accent.name }
    suspend fun setSpeechRate(rate: Float) = edit { it[Keys.SPEECH_RATE] = rate.coerceIn(0.5f, 1.5f) }
    suspend fun setSelectedVoice(voice: String?) = edit {
        if (voice == null) it.remove(Keys.VOICE) else it[Keys.VOICE] = voice
    }
    suspend fun setAutoPlay(enabled: Boolean) = edit { it[Keys.AUTO_PLAY] = enabled }
    suspend fun setShowPhonetic(show: Boolean) = edit { it[Keys.SHOW_PHONETIC] = show }
    suspend fun setDailyNewLimit(limit: Int) = edit { it[Keys.DAILY_NEW_LIMIT] = limit.coerceIn(0, 500) }
    suspend fun setSessionLength(minutes: Int) = edit { it[Keys.SESSION_LENGTH] = minutes.coerceIn(0, 600) }
    suspend fun setReminderEnabled(enabled: Boolean) = edit { it[Keys.REMINDER_ENABLED] = enabled }
    suspend fun setReminderTime(hour: Int, minute: Int) = edit {
        it[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23)
        it[Keys.REMINDER_MINUTE] = minute.coerceIn(0, 59)
    }
    suspend fun setSchedulerType(type: SchedulerType) = edit { it[Keys.SCHEDULER] = type.name }

    // --- active language pair (global workspace) ---

    val activePairId: Flow<Long> = context.dataStore.data.map { it[Keys.ACTIVE_PAIR] ?: 1L }

    suspend fun currentActivePairId(): Long = context.dataStore.data.first()[Keys.ACTIVE_PAIR] ?: 1L

    suspend fun setActivePairId(id: Long) = edit { it[Keys.ACTIVE_PAIR] = id }

    // --- AI auto-fill provider selection (the key itself lives in SecureKeyStore) ---

    val aiProviderName: Flow<String> = context.dataStore.data.map { it[Keys.AI_PROVIDER] ?: "GEMINI" }

    suspend fun currentAiProviderName(): String =
        context.dataStore.data.first()[Keys.AI_PROVIDER] ?: "GEMINI"

    suspend fun setAiProviderName(name: String) = edit { it[Keys.AI_PROVIDER] = name }

    // --- Smart Practice styles (custom prompt templates + active selection) ---

    val customPracticeStylesJson: Flow<String> =
        context.dataStore.data.map { it[Keys.PRACTICE_STYLES] ?: "" }

    suspend fun currentCustomPracticeStylesJson(): String =
        context.dataStore.data.first()[Keys.PRACTICE_STYLES] ?: ""

    suspend fun setCustomPracticeStylesJson(json: String) = edit { it[Keys.PRACTICE_STYLES] = json }

    val activePracticeStyleId: Flow<String> =
        context.dataStore.data.map { it[Keys.ACTIVE_PRACTICE_STYLE] ?: "" }

    suspend fun currentActivePracticeStyleId(): String =
        context.dataStore.data.first()[Keys.ACTIVE_PRACTICE_STYLE] ?: ""

    suspend fun setActivePracticeStyleId(id: String) = edit { it[Keys.ACTIVE_PRACTICE_STYLE] = id }

    // --- Add-card convenience: the deck the user last added a card to ---

    suspend fun currentLastDeckId(): Long = context.dataStore.data.first()[Keys.LAST_DECK] ?: -1L

    suspend fun setLastDeckId(id: Long) = edit { it[Keys.LAST_DECK] = id }

    // --- Decks excluded from global (all-decks) review and practice ---

    val excludedDeckIds: Flow<Set<Long>> =
        context.dataStore.data.map { prefs -> prefs[Keys.EXCLUDED_DECKS].toDeckIds() }

    suspend fun currentExcludedDeckIds(): Set<Long> =
        context.dataStore.data.first()[Keys.EXCLUDED_DECKS].toDeckIds()

    suspend fun setDeckIncludedInGlobal(deckId: Long, included: Boolean) = edit { prefs ->
        val current = prefs[Keys.EXCLUDED_DECKS].toDeckIds().toMutableSet()
        if (included) current.remove(deckId) else current.add(deckId)
        prefs[Keys.EXCLUDED_DECKS] = current.map { it.toString() }.toSet()
    }

    private fun Set<String>?.toDeckIds(): Set<Long> =
        this.orEmpty().mapNotNull { it.toLongOrNull() }.toSet()

    // --- one-time database seeding flag ---

    suspend fun isSeeded(): Boolean = context.dataStore.data.first()[Keys.SEEDED] ?: false

    suspend fun markSeeded() = edit { it[Keys.SEEDED] = true }

    // --- daily "new cards studied" counter (resets automatically each day) ---

    suspend fun newCardsStudiedToday(todayEpochDay: Long): Int {
        val prefs = context.dataStore.data.first()
        val storedDay = prefs[Keys.NEW_STUDIED_DAY] ?: -1L
        return if (storedDay == todayEpochDay) prefs[Keys.NEW_STUDIED_COUNT] ?: 0 else 0
    }

    fun newCardsStudiedTodayFlow(todayEpochDay: Long): Flow<Int> =
        context.dataStore.data.map { prefs ->
            val storedDay = prefs[Keys.NEW_STUDIED_DAY] ?: -1L
            if (storedDay == todayEpochDay) prefs[Keys.NEW_STUDIED_COUNT] ?: 0 else 0
        }

    suspend fun recordNewCardsStudied(count: Int, todayEpochDay: Long) {
        if (count <= 0) return
        edit { prefs ->
            val storedDay = prefs[Keys.NEW_STUDIED_DAY] ?: -1L
            val base = if (storedDay == todayEpochDay) prefs[Keys.NEW_STUDIED_COUNT] ?: 0 else 0
            prefs[Keys.NEW_STUDIED_DAY] = todayEpochDay
            prefs[Keys.NEW_STUDIED_COUNT] = base + count
        }
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private fun Preferences.toAppSettings() = AppSettings(
        themeMode = ThemeMode.fromName(this[Keys.THEME]),
        ttsAccent = TtsAccent.fromName(this[Keys.ACCENT]),
        speechRate = this[Keys.SPEECH_RATE] ?: 1.0f,
        selectedVoice = this[Keys.VOICE],
        autoPlayPronunciation = this[Keys.AUTO_PLAY] ?: true,
        showPhonetic = this[Keys.SHOW_PHONETIC] ?: true,
        dailyNewLimit = this[Keys.DAILY_NEW_LIMIT] ?: 20,
        sessionLengthMinutes = this[Keys.SESSION_LENGTH] ?: 0,
        reminderEnabled = this[Keys.REMINDER_ENABLED] ?: false,
        reminderHour = this[Keys.REMINDER_HOUR] ?: 20,
        reminderMinute = this[Keys.REMINDER_MINUTE] ?: 0,
        schedulerType = SchedulerType.fromName(this[Keys.SCHEDULER])
    )

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val ACCENT = stringPreferencesKey("tts_accent")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val VOICE = stringPreferencesKey("tts_voice")
        val AUTO_PLAY = booleanPreferencesKey("auto_play")
        val SHOW_PHONETIC = booleanPreferencesKey("show_phonetic")
        val DAILY_NEW_LIMIT = intPreferencesKey("daily_new_limit")
        val SESSION_LENGTH = intPreferencesKey("session_length")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val SCHEDULER = stringPreferencesKey("scheduler_type")
        val ACTIVE_PAIR = longPreferencesKey("active_pair_id")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val PRACTICE_STYLES = stringPreferencesKey("practice_styles")
        val ACTIVE_PRACTICE_STYLE = stringPreferencesKey("active_practice_style")
        val SEEDED = booleanPreferencesKey("seeded")
        val NEW_STUDIED_DAY = longPreferencesKey("new_studied_day")
        val NEW_STUDIED_COUNT = intPreferencesKey("new_studied_count")
        val LAST_DECK = longPreferencesKey("last_deck_id")
        val EXCLUDED_DECKS = stringSetPreferencesKey("excluded_deck_ids")
    }
}
