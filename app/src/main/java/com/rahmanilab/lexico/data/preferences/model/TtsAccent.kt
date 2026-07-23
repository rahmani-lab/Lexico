package com.rahmanilab.lexico.data.preferences.model

import java.util.Locale

enum class TtsAccent(val displayName: String, val languageTag: String) {
    US("American (en-US)", "en-US"),
    UK("British (en-GB)", "en-GB");

    fun toLocale(): Locale = Locale.forLanguageTag(languageTag)

    companion object {
        fun fromName(name: String?): TtsAccent =
            entries.firstOrNull { it.name == name } ?: US
    }
}
