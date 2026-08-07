package com.rahmanilab.lingodo.data.autofill

import com.rahmanilab.lingodo.data.repository.AiConfigRepository
import com.rahmanilab.lingodo.domain.autofill.AutoFillData
import com.rahmanilab.lingodo.domain.model.Example
import com.rahmanilab.lingodo.domain.model.WordForm
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Tiers 2–3 of the auto-fill engine: asks the user's BYOK LLM for structured card data, including a
 * translation/definition in the native (source) language and the word's full family — verb tenses
 * *and* the related noun / adjective / adverb derivatives. Returns null when no key is configured or
 * the model doesn't return usable JSON.
 */
class AiEnricher(
    private val aiConfig: AiConfigRepository,
    private val llm: LlmClient
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun enrich(
        word: String,
        sourceName: String,
        targetName: String,
        partOfSpeech: String = ""
    ): AutoFillData? {
        val provider = aiConfig.currentProvider()
        val key = aiConfig.getKey(provider)?.takeIf { it.isNotBlank() } ?: return null

        val system = "You are an expert lexicographer for the language-learning app Lingodo. " +
            "Return ONLY a raw JSON object with NO markdown formatting, NO backticks, and NO extra text."
        val raw = llm.chat(
            provider,
            provider.defaultModel,
            key,
            system,
            buildPrompt(word, sourceName, targetName, partOfSpeech.trim())
        )
        val jsonText = extractJsonObject(raw) ?: return null
        val dto = runCatching { json.decodeFromString<AiFillDto>(jsonText) }.getOrNull() ?: return null
        return dto.toData()
    }

    /**
     * The standardized prompt. [sourceName] is the native language the meaning is written in;
     * [targetName] is the study language the word belongs to.
     */
    private fun buildPrompt(
        word: String,
        sourceName: String,
        targetName: String,
        partOfSpeech: String
    ): String {
        val posRule = if (partOfSpeech.isBlank()) "" else """

        CRITICAL — PART OF SPEECH LOCK: treat "$word" STRICTLY as a $partOfSpeech.
        Every field (definition_meaning, definition, examples, synonyms, antonyms, collocations and
        word_forms) MUST describe ONLY the $partOfSpeech sense. Ignore all other senses completely —
        e.g. if asked for the noun sense of a word, never return its adjective or verb meaning.
        Set "partOfSpeech" to exactly "$partOfSpeech".
        """.trimIndent()

        return """
        Analyze the $targetName word or phrase: "$word" and return ONLY a raw JSON object with NO
        markdown formatting, NO backticks, and NO extra text.

        Translate/Define the word into: $sourceName.
        $posRule

        Rules for "word_forms":
        1. If "$word" is a VERB:
           - Include inflections: "past tense", "past participle", "gerund", "3rd person singular".
           - CRITICAL: also include related word families/derivatives: "noun", "adjective", "adverb" (if they exist).
        2. If "$word" is a NOUN / ADJECTIVE / ADVERB:
           - Include related word family forms: "verb", "noun", "adjective", "adverb" (whichever apply).
           - Include plural or comparative/superlative forms if applicable.

        Return exactly this JSON structure (use empty strings/arrays when unknown):
        {
          "definition_meaning": "clear, concise translation or definition in $sourceName",
          "phonetic": "IPA transcription of the $targetName word",
          "partOfSpeech": "one of noun, verb, adjective, adverb, phrase, idiom, phrasal verb, preposition",
          "definition": "a simple definition of the word in $targetName",
          "examples": [{"text": "an example sentence in $targetName", "translation": "its translation in $sourceName"}],
          "synonyms": ["synonym1", "synonym2", "synonym3"],
          "antonyms": ["antonym1", "antonym2"],
          "collocations": ["collocation1", "collocation2"],
          "word_forms": [{"form": "noun", "word": "..."}, {"form": "adjective", "word": "..."}, {"form": "past tense", "word": "..."}],
          "tags": ["part_of_speech", "cefr_level_or_topic"]
        }
        """.trimIndent()
    }

    /** Models sometimes wrap JSON in prose or ``` fences; take the outermost object. */
    private fun extractJsonObject(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        return if (start in 0 until end) raw.substring(start, end + 1) else null
    }

    @Serializable
    private data class AiFillDto(
        @SerialName("definition_meaning") val definitionMeaning: String = "",
        // Accepted as a fallback if a model uses the older "meaning" key.
        val meaning: String = "",
        val phonetic: String = "",
        val partOfSpeech: String = "",
        val definition: String = "",
        val examples: List<ExampleDto> = emptyList(),
        val synonyms: List<String> = emptyList(),
        val antonyms: List<String> = emptyList(),
        val collocations: List<String> = emptyList(),
        @SerialName("word_forms") val wordForms: List<WordFormDto> = emptyList(),
        val tags: List<String> = emptyList()
    ) {
        fun toData() = AutoFillData(
            phonetic = phonetic,
            partOfSpeech = partOfSpeech,
            meaning = definitionMeaning.ifBlank { meaning },
            definition = definition,
            examples = examples.filter { it.text.isNotBlank() }.map { Example(it.text, it.translation) },
            synonyms = synonyms.filter { it.isNotBlank() },
            antonyms = antonyms.filter { it.isNotBlank() },
            collocations = collocations.filter { it.isNotBlank() },
            // JSON "form" is the label (noun/past tense…) and "word" is the actual word.
            wordForms = wordForms.filter { it.word.isNotBlank() }.map { WordForm(it.form, it.word) },
            tags = tags.filter { it.isNotBlank() }
        )
    }

    @Serializable
    private data class ExampleDto(val text: String = "", val translation: String = "")

    @Serializable
    private data class WordFormDto(val form: String = "", val word: String = "")
}
