package com.rahmanilab.lingodo.domain.practice

import kotlinx.serialization.Serializable

/**
 * A Smart Practice "style" — a reusable prompt template that flavours the AI-generated drills.
 *
 * [instructions] is the editable, user-facing prompt. The practice engine always appends the fixed
 * JSON output schema (a fill-in-the-blank per word), so every style — built-in or custom — still
 * produces gradable exercises that feed back into the SRS scheduler.
 */
@Serializable
data class PracticeStyle(
    val id: String,
    val emoji: String,
    val name: String,
    val instructions: String,
    val builtIn: Boolean = false
) {
    companion object {
        const val DEFAULT_ID = "preset_fill_blank"

        val presets: List<PracticeStyle> = listOf(
            PracticeStyle(
                id = DEFAULT_ID,
                emoji = "📝",
                name = "Sentence Building & Fill-in-the-Blank",
                instructions = "Create natural, everyday sentences that each use one target word.",
                builtIn = true
            ),
            PracticeStyle(
                id = "preset_story",
                emoji = "📖",
                name = "Contextual Story & Comprehension",
                instructions = "Write sentences that together form a short, coherent story; use one target word in each sentence.",
                builtIn = true
            ),
            PracticeStyle(
                id = "preset_roleplay",
                emoji = "💬",
                name = "Roleplay & Scenario Dialogue",
                instructions = "Write short dialogue lines from a realistic conversation scenario; use one target word in each line.",
                builtIn = true
            )
        )

        fun default(): PracticeStyle = presets.first()
    }
}
