package com.rahmanilab.lingodo.domain.model

import java.util.Locale

/**
 * Languages LingoDo supports for language pairs (the study/target language and the native/source
 * explanation language). [ttsTag] is the BCP-47 tag used to switch the Android TextToSpeech locale.
 */
enum class Language(
    val code: String,
    val displayName: String,
    val ttsTag: String,
    val isRtl: Boolean = false
) {
    ENGLISH("en", "English", "en-US"),
    PERSIAN("fa", "Persian — فارسی", "fa-IR", isRtl = true),
    GERMAN("de", "German — Deutsch", "de-DE"),
    FRENCH("fr", "French — Français", "fr-FR"),
    SPANISH("es", "Spanish — Español", "es-ES"),
    ITALIAN("it", "Italian — Italiano", "it-IT"),
    PORTUGUESE("pt", "Portuguese — Português", "pt-PT"),
    DUTCH("nl", "Dutch — Nederlands", "nl-NL"),
    RUSSIAN("ru", "Russian — Русский", "ru-RU"),
    TURKISH("tr", "Turkish — Türkçe", "tr-TR"),
    ARABIC("ar", "Arabic — العربية", "ar-SA", isRtl = true),
    HINDI("hi", "Hindi — हिन्दी", "hi-IN"),
    CHINESE("zh", "Chinese — 中文", "zh-CN"),
    JAPANESE("ja", "Japanese — 日本語", "ja-JP"),
    KOREAN("ko", "Korean — 한국어", "ko-KR");

    fun toLocale(): Locale = Locale.forLanguageTag(ttsTag)

    companion object {
        fun fromCode(code: String?): Language =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}
