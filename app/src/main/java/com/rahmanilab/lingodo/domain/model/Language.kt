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
    val sampleWord: String,
    val isRtl: Boolean = false
) {
    ENGLISH("en", "English", "en-US", "resilient"),
    PERSIAN("fa", "Persian — فارسی", "fa-IR", "سلام", isRtl = true),
    GERMAN("de", "German — Deutsch", "de-DE", "Wunderbar"),
    FRENCH("fr", "French — Français", "fr-FR", "Bonjour"),
    SPANISH("es", "Spanish — Español", "es-ES", "Hola"),
    ITALIAN("it", "Italian — Italiano", "it-IT", "Ciao"),
    PORTUGUESE("pt", "Portuguese — Português", "pt-PT", "Olá"),
    DUTCH("nl", "Dutch — Nederlands", "nl-NL", "Hallo"),
    RUSSIAN("ru", "Russian — Русский", "ru-RU", "Привет"),
    TURKISH("tr", "Turkish — Türkçe", "tr-TR", "Merhaba"),
    ARABIC("ar", "Arabic — العربية", "ar-SA", "مرحبا", isRtl = true),
    HINDI("hi", "Hindi — हिन्दी", "hi-IN", "नमस्ते"),
    CHINESE("zh", "Chinese — 中文", "zh-CN", "你好"),
    JAPANESE("ja", "Japanese — 日本語", "ja-JP", "こんにちは"),
    KOREAN("ko", "Korean — 한국어", "ko-KR", "안녕하세요");

    fun toLocale(): Locale = Locale.forLanguageTag(ttsTag)

    companion object {
        fun fromCode(code: String?): Language =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}
