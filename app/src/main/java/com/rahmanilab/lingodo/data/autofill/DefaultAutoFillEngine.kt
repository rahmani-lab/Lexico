package com.rahmanilab.lingodo.data.autofill

import com.rahmanilab.lingodo.data.net.AiErrors
import com.rahmanilab.lingodo.data.repository.AiConfigRepository
import com.rahmanilab.lingodo.data.repository.DictionaryConfigRepository
import com.rahmanilab.lingodo.domain.autofill.AutoFillData
import com.rahmanilab.lingodo.domain.autofill.AutoFillEngine
import com.rahmanilab.lingodo.domain.autofill.AutoFillOutcome
import com.rahmanilab.lingodo.domain.model.DictionarySource
import com.rahmanilab.lingodo.domain.model.Language

/**
 * Live 3-tier auto-fill orchestrator:
 *  1. keyless [DictionaryClient] (English targets) for phonetics, audio, POS, examples, synonyms;
 *  2. and 3. the BYOK [AiEnricher] for the source-language meaning, translated examples,
 *     collocations, and the auto-detected grammatical class + word forms/inflections.
 *
 * Results are merged (the dictionary is authoritative for phonetics; the AI for meaning/forms).
 */
class DefaultAutoFillEngine(
    private val aiConfig: AiConfigRepository,
    private val dictionaryClient: DictionaryClient,
    private val merriamWebsterClient: MerriamWebsterClient,
    private val dictionaryConfig: DictionaryConfigRepository,
    private val aiEnricher: AiEnricher
) : AutoFillEngine {

    /**
     * Tier 1 lookup through the dictionary the user selected in Settings. A configured publisher
     * dictionary (Merriam-Webster / Oxford) is tried first and falls back to the keyless one, so a
     * missing key or a miss never blocks auto-fill.
     */
    private suspend fun lookUpDictionary(word: String, targetCode: String, pos: String): AutoFillData? {
        if (targetCode != "en") return null
        val source = dictionaryConfig.currentSource()
        val preferred = when (source) {
            DictionarySource.MERRIAM_WEBSTER_LEARNERS ->
                dictionaryConfig.getKey(source)?.takeIf { it.isNotBlank() }?.let { key ->
                    runCatching { merriamWebsterClient.lookup(word, key, pos) }.getOrNull()
                }
            // Oxford requires paid credentials; until a key is present we simply use the free tier.
            else -> null
        }
        return preferred ?: runCatching { dictionaryClient.lookup(word, pos) }.getOrNull()
    }

    override suspend fun enrich(
        word: String,
        sourceCode: String,
        targetCode: String,
        partOfSpeech: String
    ): AutoFillOutcome {
        val trimmed = word.trim()
        if (trimmed.isBlank()) return AutoFillOutcome.Unavailable("Type a word first, then tap auto-fill.")

        val sourceName = cleanName(sourceCode)
        val targetName = cleanName(targetCode)
        val pos = partOfSpeech.trim()

        val dictionary = lookUpDictionary(trimmed, targetCode, pos)

        val aiResult = runCatching { aiEnricher.enrich(trimmed, sourceName, targetName, pos) }
        val aiData = aiResult.getOrNull()

        if (dictionary == null && aiData == null) {
            return if (!aiConfig.hasKeyForCurrent()) {
                AutoFillOutcome.Unavailable(
                    "No dictionary result. Add an AI provider key in Settings → AI Auto-fill for full auto-fill."
                )
            } else {
                val error = aiResult.exceptionOrNull()
                AutoFillOutcome.Error(
                    if (error != null) AiErrors.friendlyMessage(error)
                    else "Auto-fill couldn't find data for this word."
                )
            }
        }

        return AutoFillOutcome.Success(merge(dictionary, aiData, pos))
    }

    private fun merge(dictionary: AutoFillData?, ai: AutoFillData?, pinnedPos: String): AutoFillData {
        val d = dictionary ?: AutoFillData()
        val a = ai ?: AutoFillData()
        return AutoFillData(
            phonetic = d.phonetic.ifBlank { a.phonetic },
            // A part of speech the user pinned always wins over whatever the sources report.
            partOfSpeech = pinnedPos.ifBlank { d.partOfSpeech.ifBlank { a.partOfSpeech } },
            meaning = a.meaning.ifBlank { d.meaning },
            definition = d.definition.ifBlank { a.definition },
            // Prefer AI examples (they carry translations); fall back to dictionary examples.
            examples = a.examples.ifEmpty { d.examples },
            synonyms = (d.synonyms + a.synonyms).distinct().take(8),
            antonyms = (d.antonyms + a.antonyms).distinct().take(8),
            collocations = a.collocations.ifEmpty { d.collocations },
            wordForms = a.wordForms.ifEmpty { d.wordForms },
            tags = (a.tags + d.tags).distinct().take(6),
            audioUrl = d.audioUrl ?: a.audioUrl
        )
    }

    /** "German — Deutsch" -> "German", for a clean prompt. */
    private fun cleanName(code: String): String =
        Language.fromCode(code).displayName.substringBefore(" —").trim()
}
