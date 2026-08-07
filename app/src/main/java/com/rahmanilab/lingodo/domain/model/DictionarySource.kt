package com.rahmanilab.lingodo.domain.model

/**
 * A dictionary the auto-fill engine can consult for definitions and pronunciation.
 *
 * Note on "standard" dictionaries: Longman and Oxford data is licensed and cannot be bundled with
 * or downloaded into an app. What LingoDo can do — and does here — is let you enable the reputable
 * dictionaries that publish an API and plug in your own key where one is required. Everything is
 * still fetched directly from the publisher to the device.
 */
enum class DictionarySource(
    val displayName: String,
    val description: String,
    /** True when the source works with no account at all. */
    val keyless: Boolean,
    /** Where to get a key (blank for keyless sources). */
    val keyPortalUrl: String,
    /** Language codes the source covers; empty means "any". */
    val languages: Set<String>
) {
    FREE_DICTIONARY(
        displayName = "Free Dictionary",
        description = "No account needed. Wiktionary-based definitions, IPA and audio for English.",
        keyless = true,
        keyPortalUrl = "",
        languages = setOf("en")
    ),
    MERRIAM_WEBSTER_LEARNERS(
        displayName = "Merriam-Webster Learner's",
        description = "A true learner's dictionary with clear definitions and audio. Free key after sign-up.",
        keyless = false,
        keyPortalUrl = "https://dictionaryapi.com/register/index",
        languages = setOf("en")
    ),
    OXFORD(
        displayName = "Oxford Dictionaries",
        description = "Oxford definitions and pronunciations. Requires your own (paid) Oxford API credentials.",
        keyless = false,
        keyPortalUrl = "https://developer.oxforddictionaries.com/",
        languages = setOf("en")
    );

    fun supports(languageCode: String): Boolean =
        languages.isEmpty() || languageCode.substringBefore('-') in languages

    companion object {
        fun fromName(name: String?): DictionarySource =
            entries.firstOrNull { it.name == name } ?: FREE_DICTIONARY
    }
}
