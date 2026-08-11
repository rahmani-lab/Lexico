package com.rahmanilab.lingodo.domain.practice

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The AI writing coach's verdict on a submitted piece of writing: a list of specific, actionable
 * [tips] plus a fully polished rewrite.
 *
 * Note: the project serializes with kotlinx.serialization (not Gson), so the JSON keys from the
 * spec are mapped with [SerialName] rather than `@SerializedName`.
 */
@Serializable
data class WritingAnalysis(
    val tips: List<WritingTip> = emptyList(),
    @SerialName("improved_full_text") val improvedFullText: String = ""
) {
    val hasFeedback: Boolean get() = tips.isNotEmpty() || improvedFullText.isNotBlank()
}

/** One flagged phrase, its correction, and why the change is needed. */
@Serializable
data class WritingTip(
    @SerialName("original_text") val originalText: String = "",
    val suggestion: String = "",
    val reason: String = ""
) {
    val isUsable: Boolean get() = originalText.isNotBlank() && suggestion.isNotBlank()
}

/** A writing prompt: the topic to write about plus the words the learner should try to use. */
data class WritingTopic(
    val prompt: String,
    val targetWords: List<String> = emptyList()
)

/** Where a tip's [WritingTip.originalText] was found in the submitted essay, for highlighting. */
data class TipHighlight(
    val tipIndex: Int,
    val start: Int,
    val end: Int
)

/** Maximum length of a submitted piece of writing, in words. */
const val WRITING_WORD_LIMIT = 300
