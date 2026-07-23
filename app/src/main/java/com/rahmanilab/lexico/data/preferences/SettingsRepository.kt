package com.rahmanilab.lexico.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rahmanilab.lexico.data.preferences.model.AppSettings
import com.rahmanilab.lexico.data.preferences.model.SchedulerType
import com.rahmanilab.lexico.data.preferences.model.ThemeMode
import com.rahmanilab.lexico.data.preferences.model.TtsAccent
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
        val SEEDED = booleanPreferencesKey("seeded")
        val NEW_STUDIED_DAY = longPreferencesKey("new_studied_day")
        val NEW_STUDIED_COUNT = intPreferencesKey("new_studied_count")
    }
}
