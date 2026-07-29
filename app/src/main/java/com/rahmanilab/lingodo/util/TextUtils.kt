package com.rahmanilab.lingodo.util

/**
 * Text helpers used by the typing and fill-in-the-blank review modes.
 */
object TextUtils {

    /** Normalises an answer for lenient comparison: trims, lowercases and collapses whitespace. */
    fun normalizeAnswer(input: String): String =
        input.trim().lowercase().replace(Regex("\\s+"), " ")

    fun answersMatch(a: String, b: String): Boolean =
        normalizeAnswer(a) == normalizeAnswer(b)

    /**
     * Replaces the first case-insensitive occurrence of [word] in [sentence] with a blank so a
     * cloze card can hide the target word. Falls back to appending a blank when the word is not
     * literally present (common for phrases or inflected forms).
     */
    fun clozeBlank(sentence: String, word: String, blank: String = "_____"): String {
        if (sentence.isBlank() || word.isBlank()) return sentence
        val regex = Regex(Regex.escape(word), RegexOption.IGNORE_CASE)
        return if (regex.containsMatchIn(sentence)) {
            regex.replaceFirst(sentence, blank)
        } else {
            sentence
        }
    }
}
