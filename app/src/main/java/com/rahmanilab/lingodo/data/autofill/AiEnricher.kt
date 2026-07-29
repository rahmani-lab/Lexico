package com.rahmanilab.lingodo.data.autofill

import com.rahmanilab.lingodo.data.repository.AiConfigRepository
import com.rahmanilab.lingodo.domain.autofill.AutoFillData
import com.rahmanilab.lingodo.domain.model.Example
import com.rahmanilab.lingodo.domain.model.WordForm
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Tiers 2–3 of the auto-fill engine: asks the user's BYOK LLM for structured card data, including
 * the auto-detected grammatical class and the word's inflections/derivatives. Returns null when no
 * key is configured or the model doesn't return usable JSON.
 */
class AiEnricher(
    private val aiConfig: AiConfigRepository,
    private val llm: LlmClient
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun enrich(word: String, sourceName: String, targetName: String): AutoFillData? {
        val provider = aiConfig.currentProvider()
        val key = aiConfig.getKey(provider)?.takeIf { it.isNotBlank() } ?: return null

        val system = "You are a precise lexicographer for a language-learning flashcard app. " +
            "Respond with ONLY a single minified JSON object — no markdown, no code fences, no commentary."
        val raw = llm.chat(provider, provider.defaultModel, key, system, buildPrompt(word, sourceName, targetName))
        val jsonText = extractJsonObject(raw) ?: return null
        val dto = runCatching { json.decodeFromString<AiFillDto>(jsonText) }.getOrNull() ?: return null
        return dto.toData()
    }

    private fun buildPrompt(word: String, sourceName: String, targetName: String): String = """
        For the $targetName word or phrase: "$word".
        Return a JSON object with exactly these keys:
        "phonetic": IPA transcription of the $targetName word,
        "partOfSpeech": one of noun, verb, adjective, adverb, phrase, idiom, phrasal verb, preposition,
        "meaning": a short meaning/translation in $sourceName,
        "definition": a simple definition in $targetName,
        "examples": array of up to 2 objects {"text": example sentence in $targetName, "translation": its translation in $sourceName},
        "synonyms": array of $targetName synonyms,
        "antonyms": array of $targetName antonyms,
        "collocations": array of common $targetName collocations,
        "wordForms": array of {"label","form"} — FIRST detect the grammatical class, then list the relevant inflections
        (verbs: past, past participle, 3rd person, gerund; adjectives: comparative, superlative; nouns: plural)
        plus notable word-family derivatives.
        Use empty strings/arrays when unknown. Output JSON only.
    """.trimIndent()

    /** Models sometimes wrap JSON in prose or ``` fences; take the outermost object. */
    private fun extractJsonObject(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        return if (start in 0 until end) raw.substring(start, end + 1) else null
    }

    @Serializable
    private data class AiFillDto(
        val phonetic: String = "",
        val partOfSpeech: String = "",
        val meaning: String = "",
        val definition: String = "",
        val examples: List<ExampleDto> = emptyList(),
        val synonyms: List<String> = emptyList(),
        val antonyms: List<String> = emptyList(),
        val collocations: List<String> = emptyList(),
        val wordForms: List<WordFormDto> = emptyList()
    ) {
        fun toData() = AutoFillData(
            phonetic = phonetic,
            partOfSpeech = partOfSpeech,
            meaning = meaning,
            definition = definition,
            examples = examples.filter { it.text.isNotBlank() }.map { Example(it.text, it.translation) },
            synonyms = synonyms.filter { it.isNotBlank() },
            antonyms = antonyms.filter { it.isNotBlank() },
            collocations = collocations.filter { it.isNotBlank() },
            wordForms = wordForms.filter { it.form.isNotBlank() }.map { WordForm(it.label, it.form) }
        )
    }

    @Serializable
    private data class ExampleDto(val text: String = "", val translation: String = "")

    @Serializable
    private data class WordFormDto(val label: String = "", val form: String = "")
}
