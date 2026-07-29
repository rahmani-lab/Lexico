package com.rahmanilab.lingodo.domain.practice

/**
 * A snapshot of the learner's state for the active language pair, used to build adaptive AI prompts.
 */
data class PracticeContext(
    val pairId: Long,
    val summary: String,
    val troublesome: List<String>,
    val mastered: List<String>,
    val targetLanguage: String,
    val sourceLanguage: String
) {
    val hasWords: Boolean get() = troublesome.isNotEmpty() || mastered.isNotEmpty()
}

/**
 * One AI-generated fill-in-the-blank drill. Answering it closes the loop back into the SRS via the
 * matched [cardId].
 */
data class PracticeExercise(
    val prompt: String,
    val answer: String,
    val translation: String = "",
    val cardId: Long = -1L
)
