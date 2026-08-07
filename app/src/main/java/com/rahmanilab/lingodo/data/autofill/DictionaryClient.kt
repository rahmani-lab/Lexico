package com.rahmanilab.lingodo.data.autofill

import com.rahmanilab.lingodo.data.net.HttpJson
import com.rahmanilab.lingodo.domain.autofill.AutoFillData
import com.rahmanilab.lingodo.domain.model.Example
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLEncoder

/**
 * Tier 1 of the auto-fill engine: the keyless, free **Dictionary API** (dictionaryapi.dev).
 * English target words only. Returns phonetics (IPA), an audio URL, part of speech, a definition,
 * example sentences and synonyms/antonyms. Returns null when the word isn't found.
 */
class DictionaryClient {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * @param partOfSpeech when non-blank, only senses with that exact part of speech are used, so
     *   "firm" as a noun never returns the adjective's definition. Falls back to every sense when
     *   the dictionary has no entry for the requested class.
     */
    suspend fun lookup(word: String, partOfSpeech: String = ""): AutoFillData? {
        val encoded = URLEncoder.encode(word.trim(), "UTF-8")
        val response = runCatching { HttpJson.get("$BASE$encoded") }.getOrNull() ?: return null
        val entries = runCatching { json.decodeFromString<List<DictEntry>>(response) }.getOrNull()
            ?: return null
        if (entries.isEmpty()) return null

        val phonetic = entries.firstNotNullOfOrNull { e ->
            e.phonetic?.takeIf { it.isNotBlank() }
                ?: e.phonetics.firstOrNull { !it.text.isNullOrBlank() }?.text
        }.orEmpty()
        val audio = entries.flatMap { it.phonetics }
            .firstOrNull { !it.audio.isNullOrBlank() }?.audio

        // Restrict to the requested part of speech when the user pinned one.
        val allMeanings = entries.flatMap { it.meanings }
        val wanted = partOfSpeech.trim()
        val meanings = if (wanted.isNotBlank()) {
            allMeanings.filter { it.partOfSpeech.equals(wanted, ignoreCase = true) }
                .ifEmpty { allMeanings }
        } else {
            allMeanings
        }

        val firstMeaning = meanings.firstOrNull()
        val definition = firstMeaning?.definitions?.firstOrNull()?.definition.orEmpty()
        val examples = meanings
            .flatMap { it.definitions }
            .mapNotNull { it.example?.takeIf { ex -> ex.isNotBlank() } }
            .distinct()
            .take(2)
            .map { Example(text = it, translation = "") }
        val synonyms = meanings
            .flatMap { it.synonyms + it.definitions.flatMap { d -> d.synonyms } }
            .filter { it.isNotBlank() }.distinct().take(8)
        val antonyms = meanings
            .flatMap { it.antonyms + it.definitions.flatMap { d -> d.antonyms } }
            .filter { it.isNotBlank() }.distinct().take(8)

        return AutoFillData(
            phonetic = phonetic,
            partOfSpeech = firstMeaning?.partOfSpeech.orEmpty(),
            definition = definition,
            examples = examples,
            synonyms = synonyms,
            antonyms = antonyms,
            audioUrl = audio
        )
    }

    private companion object {
        const val BASE = "https://api.dictionaryapi.dev/api/v2/entries/en/"
    }

    @Serializable
    private data class DictEntry(
        val word: String = "",
        val phonetic: String? = null,
        val phonetics: List<Phonetic> = emptyList(),
        val meanings: List<Meaning> = emptyList()
    )

    @Serializable
    private data class Phonetic(val text: String? = null, val audio: String? = null)

    @Serializable
    private data class Meaning(
        val partOfSpeech: String = "",
        val definitions: List<Definition> = emptyList(),
        val synonyms: List<String> = emptyList(),
        val antonyms: List<String> = emptyList()
    )

    @Serializable
    private data class Definition(
        val definition: String = "",
        val example: String? = null,
        val synonyms: List<String> = emptyList(),
        val antonyms: List<String> = emptyList()
    )
}
