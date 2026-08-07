package com.rahmanilab.lingodo.data.autofill

import com.rahmanilab.lingodo.data.net.HttpJson
import com.rahmanilab.lingodo.domain.autofill.AutoFillData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLEncoder

/**
 * Merriam-Webster's **Learner's Dictionary** API — a genuine learner-focused dictionary with clear
 * definitions and recorded audio, free for personal use once you register for a key.
 *
 * Definitions come back as short-form text ("shortdef"); audio is assembled from the documented
 * subdirectory rules. Returns null when the word isn't found or the response is a spelling
 * suggestion list rather than entries.
 */
class MerriamWebsterClient {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun lookup(word: String, apiKey: String, partOfSpeech: String = ""): AutoFillData? {
        if (apiKey.isBlank()) return null
        val encoded = URLEncoder.encode(word.trim(), "UTF-8")
        val url = "$BASE$encoded?key=${URLEncoder.encode(apiKey, "UTF-8")}"
        val response = runCatching { HttpJson.get(url) }.getOrNull() ?: return null

        // A miss returns a JSON array of suggestion strings, which fails entry decoding.
        val entries = runCatching { json.decodeFromString<List<MwEntry>>(response) }.getOrNull()
            ?: return null
        if (entries.isEmpty()) return null

        val wanted = partOfSpeech.trim()
        val matching = if (wanted.isNotBlank()) {
            entries.filter { it.functionalLabel.equals(wanted, ignoreCase = true) }.ifEmpty { entries }
        } else {
            entries
        }

        val entry = matching.firstOrNull() ?: return null
        val definition = entry.shortDefinitions.firstOrNull().orEmpty()
        if (definition.isBlank()) return null

        return AutoFillData(
            phonetic = entry.headword?.pronunciations?.firstOrNull()?.text.orEmpty(),
            partOfSpeech = entry.functionalLabel,
            definition = definition,
            audioUrl = entry.headword?.pronunciations
                ?.firstNotNullOfOrNull { it.sound?.audio }
                ?.let { audioUrl(it) }
        )
    }

    /**
     * Merriam-Webster stores audio under a subdirectory derived from the filename: "bix" for names
     * starting with `bix`, "gg" for `gg`, the first character when it is a digit or punctuation,
     * and otherwise the first letter.
     */
    private fun audioUrl(audio: String): String {
        val sub = when {
            audio.startsWith("bix") -> "bix"
            audio.startsWith("gg") -> "gg"
            audio.firstOrNull()?.isLetterOrDigit() == false -> "number"
            audio.firstOrNull()?.isDigit() == true -> "number"
            else -> audio.take(1)
        }
        return "https://media.merriam-webster.com/audio/prons/en/us/mp3/$sub/$audio.mp3"
    }

    private companion object {
        const val BASE = "https://www.dictionaryapi.com/api/v3/references/learners/json/"
    }

    @Serializable
    private data class MwEntry(
        @SerialName("fl") val functionalLabel: String = "",
        @SerialName("shortdef") val shortDefinitions: List<String> = emptyList(),
        @SerialName("hwi") val headword: MwHeadword? = null
    )

    @Serializable
    private data class MwHeadword(
        @SerialName("prs") val pronunciations: List<MwPronunciation> = emptyList()
    )

    @Serializable
    private data class MwPronunciation(
        @SerialName("mw") val text: String = "",
        val sound: MwSound? = null
    )

    @Serializable
    private data class MwSound(val audio: String = "")
}
