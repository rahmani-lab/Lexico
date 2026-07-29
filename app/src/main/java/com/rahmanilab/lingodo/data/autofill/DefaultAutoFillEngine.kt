package com.rahmanilab.lingodo.data.autofill

import com.rahmanilab.lingodo.data.repository.AiConfigRepository
import com.rahmanilab.lingodo.domain.autofill.AutoFillData
import com.rahmanilab.lingodo.domain.autofill.AutoFillEngine
import com.rahmanilab.lingodo.domain.autofill.AutoFillOutcome
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
    private val aiEnricher: AiEnricher
) : AutoFillEngine {

    override suspend fun enrich(word: String, sourceCode: String, targetCode: String): AutoFillOutcome {
        val trimmed = word.trim()
        if (trimmed.isBlank()) return AutoFillOutcome.Unavailable("Type a word first, then tap auto-fill.")

        val sourceName = cleanName(sourceCode)
        val targetName = cleanName(targetCode)

        val dictionary = if (targetCode == "en") {
            runCatching { dictionaryClient.lookup(trimmed) }.getOrNull()
        } else {
            null
        }

        val aiResult = runCatching { aiEnricher.enrich(trimmed, sourceName, targetName) }
        val aiData = aiResult.getOrNull()

        if (dictionary == null && aiData == null) {
            return if (!aiConfig.hasKeyForCurrent()) {
                AutoFillOutcome.Unavailable(
                    "No dictionary result. Add an AI provider key in Settings → AI Auto-fill for full auto-fill."
                )
            } else {
                AutoFillOutcome.Error(
                    aiResult.exceptionOrNull()?.message ?: "Auto-fill couldn't find data for this word."
                )
            }
        }

        return AutoFillOutcome.Success(merge(dictionary, aiData))
    }

    private fun merge(dictionary: AutoFillData?, ai: AutoFillData?): AutoFillData {
        val d = dictionary ?: AutoFillData()
        val a = ai ?: AutoFillData()
        return AutoFillData(
            phonetic = d.phonetic.ifBlank { a.phonetic },
            partOfSpeech = d.partOfSpeech.ifBlank { a.partOfSpeech },
            meaning = a.meaning.ifBlank { d.meaning },
            definition = d.definition.ifBlank { a.definition },
            // Prefer AI examples (they carry translations); fall back to dictionary examples.
            examples = a.examples.ifEmpty { d.examples },
            synonyms = (d.synonyms + a.synonyms).distinct().take(8),
            antonyms = (d.antonyms + a.antonyms).distinct().take(8),
            collocations = a.collocations.ifEmpty { d.collocations },
            wordForms = a.wordForms.ifEmpty { d.wordForms },
            audioUrl = d.audioUrl ?: a.audioUrl
        )
    }

    /** "German — Deutsch" -> "German", for a clean prompt. */
    private fun cleanName(code: String): String =
        Language.fromCode(code).displayName.substringBefore(" —").trim()
}
