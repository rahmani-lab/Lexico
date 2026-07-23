package com.rahmanilab.lexico.data.preferences.model

/**
 * All user-tunable settings, persisted in DataStore (never in the Room database, per the Android
 * guidance for small key–value / preference data).
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val ttsAccent: TtsAccent = TtsAccent.US,
    /** 0.5f – 1.5f, where 1.0f is the engine's normal speed. */
    val speechRate: Float = 1.0f,
    /** Specific TTS voice name, or null to let the engine pick one for the accent. */
    val selectedVoice: String? = null,
    val autoPlayPronunciation: Boolean = true,
    val showPhonetic: Boolean = true,
    val dailyNewLimit: Int = 20,
    /** Length of a study session in minutes; 0 means unlimited. */
    val sessionLengthMinutes: Int = 0,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val schedulerType: SchedulerType = SchedulerType.FSRS
)
