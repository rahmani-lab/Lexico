package com.rahmanilab.lexico.domain.model

/**
 * How a card is presented during a study session.
 */
enum class ReviewMode(val label: String, val description: String) {
    /** Show the English word, recall the meaning. The default. */
    FRONT_TO_BACK("Word → Meaning", "See the word, recall the meaning"),

    /** Show the meaning, recall the English word (reverse practice). */
    BACK_TO_FRONT("Meaning → Word", "See the meaning, recall the word"),

    /** Show the meaning and type the English word; the answer is checked automatically. */
    TYPING("Type the word", "Type the answer and compare it to the card"),

    /** Show an example sentence with the word blanked out and recall the missing word. */
    CLOZE("Fill in the blank", "Recall the word missing from an example sentence");

    companion object {
        val default = FRONT_TO_BACK
    }
}
