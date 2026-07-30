package com.rahmanilab.lingodo.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import com.rahmanilab.lingodo.data.preferences.model.TtsAccent
import com.rahmanilab.lingodo.domain.model.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Thin, app-scoped wrapper around Android's built-in [TextToSpeech] engine.
 *
 * The engine is created once and kept alive for the whole process (pronunciation happens on many
 * screens), which also avoids the noticeable initialization delay you'd get by recreating it per
 * screen. Call [release] from [android.app.Application] teardown if you ever need to.
 *
 * Playback is locale-driven: callers resolve the correct [Locale] for the word being spoken (via
 * [resolveTtsLocale], which maps the card/pair's target language and honours the US/UK accent only
 * for English), so studying French speaks with a French voice, German with a German voice, etc.
 */
class PronunciationManager(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            _isReady.value = true
        }
    }

    /**
     * Speak [text] in [locale], optionally with a specific [voiceName], at [rate] (1.0 = normal).
     * Safe to call before the engine is ready — it simply no-ops.
     */
    fun speak(
        text: String,
        locale: Locale = Locale.US,
        voiceName: String? = null,
        rate: Float = 1.0f
    ) {
        val engine = tts ?: return
        if (!_isReady.value || text.isBlank()) return

        engine.language = locale
        if (voiceName != null) {
            runCatching {
                engine.voices?.firstOrNull { it.name == voiceName }?.let { engine.voice = it }
            }
        }
        engine.setSpeechRate(rate.coerceIn(0.1f, 2.0f))
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId(text))
    }

    /** Speak at half speed, for careful listening. */
    fun speakSlow(
        text: String,
        locale: Locale = Locale.US,
        voiceName: String? = null,
        baseRate: Float = 1.0f
    ) = speak(text, locale, voiceName, baseRate * 0.5f)

    /**
     * Installed voices for [languageTag]'s language (e.g. "en-US" → English voices), for the Settings
     * voice picker. Falls back to an empty list when nothing local is available.
     */
    fun availableVoices(languageTag: String): List<String> {
        val lang = Locale.forLanguageTag(languageTag).language
        return runCatching {
            tts?.voices
                ?.filter { it.locale.language == lang && !it.isNetworkConnectionRequired }
                ?.map { it.name }
                ?.distinct()
                ?.sorted()
                .orEmpty()
        }.getOrDefault(emptyList())
    }

    fun stop() {
        tts?.stop()
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isReady.value = false
    }

    private fun utteranceId(text: String): String = "lingodo_${text.hashCode()}"

    companion object {
        /**
         * The [Locale] to speak a word tagged with [languageCode] in. English honours the user's
         * US/UK [accent]; every other language uses its own default locale.
         */
        fun resolveTtsLocale(languageCode: String, accent: TtsAccent): Locale {
            val language = Language.fromCode(languageCode)
            return if (language == Language.ENGLISH) accent.toLocale() else language.toLocale()
        }
    }
}
