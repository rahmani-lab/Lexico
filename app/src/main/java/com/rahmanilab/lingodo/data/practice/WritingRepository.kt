package com.rahmanilab.lingodo.data.practice

import com.rahmanilab.lingodo.data.autofill.LlmClient
import com.rahmanilab.lingodo.data.local.LingoDoDatabase
import com.rahmanilab.lingodo.data.repository.AiConfigRepository
import com.rahmanilab.lingodo.data.repository.ReviewRepository
import com.rahmanilab.lingodo.domain.model.Rating
import com.rahmanilab.lingodo.domain.practice.TipHighlight
import com.rahmanilab.lingodo.domain.practice.WritingAnalysis
import com.rahmanilab.lingodo.domain.practice.WritingTopic
import kotlinx.serialization.json.Json

/**
 * The Writing & Grammar coach. Builds a topic from the learner's own vocabulary, sends their essay
 * to the configured BYOK provider for structured feedback, and — like the rest of Smart Practice —
 * feeds the outcome back into the SRS: target words used correctly are rewarded.
 */
class WritingRepository(
    private val db: LingoDoDatabase,
    private val practiceRepository: PracticeRepository,
    private val aiConfig: AiConfigRepository,
    private val llm: LlmClient,
    private val reviewRepository: ReviewRepository
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Today's topic. Built from the learner's troublesome/mastered words when there are any, so the
     * drill exercises the vocabulary they are actually studying; otherwise a general prompt.
     */
    suspend fun buildTopic(): WritingTopic {
        val ctx = practiceRepository.buildContext()
        val words = (ctx.troublesome + ctx.mastered).distinct().take(5)
        return if (words.isEmpty()) {
            WritingTopic(
                prompt = "Describe a place you know well and why it matters to you.",
                targetWords = emptyList()
            )
        } else {
            WritingTopic(
                prompt = "Write a short piece that naturally uses these words: " +
                    "${words.joinToString(", ")}. Say what they mean to you or invent a small story.",
                targetWords = words
            )
        }
    }

    /**
     * Send [essay] for analysis against [topic]. Returns structured feedback, or a failure whose
     * message the caller maps through AiErrors.
     */
    suspend fun analyze(topic: WritingTopic, essay: String): Result<WritingAnalysis> {
        val text = essay.trim()
        if (text.isBlank()) {
            return Result.failure(IllegalStateException("Write something first, then tap Check My Writing."))
        }
        val ctx = practiceRepository.buildContext()
        val provider = aiConfig.currentProvider()
        val key = aiConfig.getKey(provider)?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("Add an AI key in Settings → AI Auto-fill."))

        val system = """
            You are an expert ${ctx.targetLanguage} writing coach.
            Analyze the user's submitted text based on grammar, spelling, word choice, and sentence
            structure against the given topic.

            Identify all errors and areas for improvement.

            For each issue found, return:
            1. "original_text": The exact incorrect or weak phrase copied verbatim from the user's input.
            2. "suggestion": The corrected or improved version.
            3. "reason": A short, clear explanation of why the change is needed.

            Also provide "improved_full_text" containing the fully polished version of the entire response.

            Every "original_text" MUST appear character-for-character in the user's text so it can be
            highlighted. Write "reason" in ${ctx.sourceLanguage}; keep "original_text", "suggestion"
            and "improved_full_text" in ${ctx.targetLanguage}. Use no other language or script.

            Respond ONLY with a raw JSON object in this format, with no markdown and no backticks:
            {"tips":[{"original_text":"...","suggestion":"...","reason":"..."}],"improved_full_text":"..."}
        """.trimIndent()

        val user = buildString {
            append("Topic: ${topic.prompt}\n\n")
            if (topic.targetWords.isNotEmpty()) {
                append("Target words: ${topic.targetWords.joinToString(", ")}\n\n")
            }
            append("User's text:\n$text")
        }

        val raw = runCatching { llm.chat(provider, provider.defaultModel, key, system, user) }
            .getOrElse { return Result.failure(it) }
        val objectJson = extractJsonObject(raw)
            ?: return Result.failure(IllegalStateException("Couldn't read the AI response."))
        val analysis = runCatching { json.decodeFromString<WritingAnalysis>(objectJson) }
            .getOrElse { return Result.failure(it) }

        return if (analysis.hasFeedback) {
            Result.success(analysis.copy(tips = analysis.tips.filter { it.isUsable }))
        } else {
            Result.failure(IllegalStateException("The coach didn't return any feedback. Try again."))
        }
    }

    /**
     * Reward target words the learner used without the coach flagging them: each counts as a GOOD
     * answer, nudging that card's stability up. Words that appear inside a flagged phrase are left
     * alone rather than penalised — writing is harder than recall, so this only ever helps.
     */
    suspend fun rewardCorrectlyUsedWords(
        topic: WritingTopic,
        essay: String,
        analysis: WritingAnalysis
    ): List<String> {
        if (topic.targetWords.isEmpty() || essay.isBlank()) return emptyList()
        val pairId = practiceRepository.buildContext().pairId
        val flagged = analysis.tips.joinToString(" ") { it.originalText }.lowercase()
        val rewarded = mutableListOf<String>()

        for (word in topic.targetWords) {
            if (!containsWord(essay, word)) continue
            if (flagged.contains(word.lowercase())) continue
            val cardId = db.cardDao().findCardIdByWordInPair(pairId, word) ?: continue
            db.cardDao().getCardWithDetails(cardId)?.let { card ->
                reviewRepository.answer(card, Rating.GOOD)
                rewarded += word
            }
        }
        return rewarded
    }

    /** Character ranges of each tip's flagged phrase inside [essay], for highlighting. */
    fun highlightsFor(essay: String, analysis: WritingAnalysis): List<TipHighlight> =
        analysis.tips.mapIndexedNotNull { index, tip ->
            val start = essay.indexOf(tip.originalText, ignoreCase = true)
            if (start < 0) null else TipHighlight(index, start, start + tip.originalText.length)
        }.sortedBy { it.start }

    /** Whole-word containment, so "act" doesn't match "contract". */
    private fun containsWord(text: String, word: String): Boolean =
        Regex("(?<![\\p{L}])${Regex.escape(word)}(?![\\p{L}])", RegexOption.IGNORE_CASE)
            .containsMatchIn(text)

    private fun extractJsonObject(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        return if (start in 0 until end) raw.substring(start, end + 1) else null
    }
}
