package com.rahmanilab.lingodo.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import com.rahmanilab.lingodo.data.preferences.model.TtsAccent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thin, app-scoped wrapper around Android's built-in [TextToSpeech] engine.
 *
 * The engine is created once and kept alive for the whole process (pronunciation happens on many
 * screens), which also avoids the noticeable initialization delay you'd get by recreating it per
 * screen. Call [release] from [android.app.Application] teardown if you ever need to.
 */
class PronunciationManager(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = TtsAccent.US.toLocale()
            _isReady.value = true
        }
    }

    /**
     * Speak [text] using the given [accent], optionally a specific [voiceName], at [rate] (1.0 =
     * normal). Safe to call before the engine is ready — it simply no-ops.
     */
    fun speak(
        text: String,
        accent: TtsAccent = TtsAccent.US,
        voiceName: String? = null,
        rate: Float = 1.0f
    ) {
        val engine = tts ?: return
        if (!_isReady.value || text.isBlank()) return

        engine.language = accent.toLocale()
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
        accent: TtsAccent = TtsAccent.US,
        voiceName: String? = null,
        baseRate: Float = 1.0f
    ) = speak(text, accent, voiceName, baseRate * 0.5f)

    /** Names of the installed English voices, for the Settings voice picker. */
    fun availableEnglishVoices(): List<String> =
        runCatching {
            tts?.voices
                ?.filter { it.locale.language == "en" && !it.isNetworkConnectionRequired }
                ?.map { it.name }
                ?.distinct()
                ?.sorted()
                .orEmpty()
        }.getOrDefault(emptyList())

    fun stop() {
        tts?.stop()
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isReady.value = false
    }

    private fun utteranceId(text: String): String = "lexico_${text.hashCode()}"
}
