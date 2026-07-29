package com.rahmanilab.lingodo.data.practice

import com.rahmanilab.lingodo.data.autofill.LlmClient
import com.rahmanilab.lingodo.data.local.LingoDoDatabase
import com.rahmanilab.lingodo.data.repository.AiConfigRepository
import com.rahmanilab.lingodo.data.repository.LanguagePairRepository
import com.rahmanilab.lingodo.data.repository.ReviewRepository
import com.rahmanilab.lingodo.data.repository.StatsRepository
import com.rahmanilab.lingodo.domain.model.Rating
import com.rahmanilab.lingodo.domain.practice.PracticeContext
import com.rahmanilab.lingodo.domain.practice.PracticeExercise
import com.rahmanilab.lingodo.domain.practice.PracticeStyle
import com.rahmanilab.lingodo.util.TextUtils
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The Adaptive Practice engine. It reads the local SRS data to find troublesome/mastered words,
 * turns them into adaptive prompts for the BYOK LLM, and grades answers back into the scheduler —
 * a closed feedback loop that keeps everything on-device except the direct LLM call.
 */
class PracticeRepository(
    private val db: LingoDoDatabase,
    private val statsRepository: StatsRepository,
    private val languagePairRepository: LanguagePairRepository,
    private val aiConfig: AiConfigRepository,
    private val llm: LlmClient,
    private val reviewRepository: ReviewRepository
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun buildContext(): PracticeContext {
        val pair = languagePairRepository.activePair()
        val stats = statsRepository.computeStatistics()
        val troublesome = db.cardDao().getTroublesomeCards(pair.id, WORD_LIMIT).map { it.card.word }
        val mastered = db.cardDao().getMasteredCards(pair.id, MATURE_DAYS, WORD_LIMIT).map { it.card.word }
        val summary = buildString {
            append("You've completed ${stats.totalReviews} reviews at ${stats.correctRatePercent}% accuracy. ")
            append("Current streak: ${stats.currentStreak} day(s); learned ${stats.learnedCount} words (${stats.matureCount} mature). ")
            if (troublesome.isNotEmpty()) {
                append("Focus words: ${troublesome.joinToString(", ")}.")
            } else {
                append("No troublesome words right now — nice work!")
            }
        }
        return PracticeContext(
            pairId = pair.id,
            summary = summary,
            troublesome = troublesome,
            mastered = mastered,
            targetLanguage = clean(pair.target.displayName),
            sourceLanguage = clean(pair.source.displayName)
        )
    }

    /** AI-written motivational progress note. */
    suspend fun generateReport(): Result<String> {
        val ctx = buildContext()
        val provider = aiConfig.currentProvider()
        val key = aiConfig.getKey(provider)?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("Add an AI key in Settings → AI Auto-fill."))
        val system = "You are an encouraging language coach. Reply in plain text, 3–4 sentences, no markdown."
        val user = "Write a motivating progress note for a ${ctx.targetLanguage} learner. Data: ${ctx.summary} " +
            "Mastered: ${ctx.mastered.joinToString(", ").ifBlank { "none yet" }}. " +
            "Struggling with: ${ctx.troublesome.joinToString(", ").ifBlank { "nothing" }}."
        return runCatching { llm.chat(provider, provider.defaultModel, key, system, user).trim() }
    }

    /**
     * Generate adaptive fill-in-the-blank drills from the learner's own words, following the chosen
     * practice style. [styleInstructions] flavours the content; the JSON output schema is fixed so
     * every style stays gradable and feeds the SRS loop.
     */
    suspend fun generateExercises(
        styleInstructions: String = PracticeStyle.default().instructions,
        count: Int = 5
    ): Result<List<PracticeExercise>> {
        val ctx = buildContext()
        if (!ctx.hasWords) {
            return Result.failure(IllegalStateException("Study a few cards first to unlock Smart Practice."))
        }
        val provider = aiConfig.currentProvider()
        val key = aiConfig.getKey(provider)?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("Add an AI key in Settings → AI Auto-fill."))

        val words = (ctx.troublesome + ctx.mastered).distinct().take(count)
        val system = "You are a ${ctx.targetLanguage} teacher. Respond with ONLY a minified JSON array, no markdown."
        val user = "$styleInstructions\n\n" +
            "Practice these ${ctx.targetLanguage} words: ${words.joinToString(", ")}. " +
            "Return a JSON array; each element: {\"sentence\": one ${ctx.targetLanguage} sentence in the style above " +
            "with the target word replaced by \"_____\", \"answer\": the missing word, " +
            "\"translation\": the full sentence translated into ${ctx.sourceLanguage}}. " +
            "Exactly one element per word. JSON array only."

        val raw = runCatching { llm.chat(provider, provider.defaultModel, key, system, user) }
            .getOrElse { return Result.failure(it) }
        val arr = extractJsonArray(raw)
            ?: return Result.failure(IllegalStateException("Couldn't read the AI response."))
        val dtos = runCatching { json.decodeFromString<List<ExerciseDto>>(arr) }
            .getOrElse { return Result.failure(it) }

        val exercises = dtos.mapNotNull { dto ->
            val answer = dto.answer.trim()
            val sentence = dto.sentence.trim()
            if (answer.isBlank() || sentence.isBlank()) return@mapNotNull null
            PracticeExercise(
                prompt = sentence,
                answer = answer,
                translation = dto.translation.trim(),
                cardId = db.cardDao().findCardIdByWordInPair(ctx.pairId, answer) ?: -1L
            )
        }
        return if (exercises.isEmpty()) {
            Result.failure(IllegalStateException("No exercises were generated."))
        } else {
            Result.success(exercises)
        }
    }

    /** Grade the answer and feed the result back into the SRS scheduler for the matched card. */
    suspend fun submitAnswer(exercise: PracticeExercise, userAnswer: String): Boolean {
        val correct = TextUtils.answersMatch(userAnswer, exercise.answer)
        if (exercise.cardId > 0) {
            db.cardDao().getCardWithDetails(exercise.cardId)?.let { card ->
                reviewRepository.answer(card, if (correct) Rating.GOOD else Rating.AGAIN)
            }
        }
        return correct
    }

    private fun clean(displayName: String) = displayName.substringBefore(" —").trim()

    private fun extractJsonArray(raw: String): String? {
        val start = raw.indexOf('[')
        val end = raw.lastIndexOf(']')
        return if (start in 0 until end) raw.substring(start, end + 1) else null
    }

    private companion object {
        const val WORD_LIMIT = 8
        const val MATURE_DAYS = 21
    }

    @Serializable
    private data class ExerciseDto(
        val sentence: String = "",
        val answer: String = "",
        val translation: String = ""
    )
}
