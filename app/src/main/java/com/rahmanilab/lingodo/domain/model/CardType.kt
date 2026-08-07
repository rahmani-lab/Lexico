package com.rahmanilab.lingodo.domain.model

/**
 * What kind of card this is.
 *
 * Both types share the same two required fields, so the review engine and scheduler treat them
 * identically — only the labels and which extra fields are offered differ.
 */
enum class CardType(val label: String) {
    /** A vocabulary entry: `word` is the term, `meaning` its translation, plus phonetics, forms… */
    VOCABULARY("Vocabulary"),

    /**
     * A free-form card for grammar rules, sentences or anything else: `word` holds the front text
     * and `meaning` the back text, both arbitrary. Vocabulary-only fields are hidden.
     */
    FREEFORM("General / grammar");

    companion object {
        fun fromName(name: String?): CardType = entries.firstOrNull { it.name == name } ?: VOCABULARY
    }
}
