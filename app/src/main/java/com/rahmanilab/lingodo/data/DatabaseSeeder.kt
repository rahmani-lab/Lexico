package com.rahmanilab.lingodo.data

import com.rahmanilab.lingodo.data.local.entity.CardEntity
import com.rahmanilab.lingodo.data.preferences.SettingsRepository
import com.rahmanilab.lingodo.data.repository.CardRepository
import com.rahmanilab.lingodo.data.repository.DeckRepository
import com.rahmanilab.lingodo.data.repository.LanguagePairRepository
import com.rahmanilab.lingodo.domain.model.Example

/**
 * Populates a default Persian→English language pair and a small "Getting Started" deck the first
 * time the app runs so the UI is never empty. Guarded by a DataStore flag.
 */
class DatabaseSeeder(
    private val deckRepository: DeckRepository,
    private val cardRepository: CardRepository,
    private val settingsRepository: SettingsRepository,
    private val languagePairRepository: LanguagePairRepository
) {

    suspend fun seedIfNeeded() {
        // Always make sure a language pair exists and is active.
        val pair = languagePairRepository.ensureDefault()

        if (settingsRepository.isSeeded()) return
        if (deckRepository.deckCount() > 0) {
            settingsRepository.markSeeded()
            return
        }

        val deckId = deckRepository.createDeck(
            name = "Getting Started",
            description = "A few example cards to explore LingoDo.",
            languagePairId = pair.id
        )
        sampleCards(deckId).forEach { (card, tags) -> cardRepository.addCard(card, tags) }
        settingsRepository.markSeeded()
    }

    private fun sampleCards(deckId: Long): List<Pair<CardEntity, List<String>>> = listOf(
        card(
            deckId = deckId,
            word = "resilient",
            partOfSpeech = "adjective",
            phonetic = "/rɪˈzɪliənt/",
            pronunciationHint = "ri-ZIL-ee-uhnt",
            meaning = "مقاوم، دارای توان بازیابی",
            englishDefinition = "Able to recover quickly after difficulty or damage.",
            examples = listOf(
                Example("Children are often more resilient than adults.", "کودکان اغلب از بزرگسالان انعطاف‌پذیرتر هستند."),
                Example("A resilient economy bounces back after a crisis.", "اقتصاد مقاوم پس از بحران به‌سرعت بازمی‌گردد.")
            ),
            synonyms = listOf("tough", "adaptable", "hardy"),
            antonyms = listOf("fragile", "vulnerable"),
            collocations = listOf("resilient system", "resilient economy", "emotionally resilient"),
            notes = "در مقالات مهندسی هم برای سیستم‌های مقاوم استفاده می‌شود.",
            source = "Newspaper article"
        ) to listOf("adjective", "IELTS"),

        card(
            deckId = deckId,
            word = "meticulous",
            partOfSpeech = "adjective",
            phonetic = "/məˈtɪkjələs/",
            pronunciationHint = "muh-TIK-yuh-luhs",
            meaning = "دقیق، موشکاف",
            englishDefinition = "Very careful and precise about small details.",
            examples = listOf(
                Example("She is meticulous about keeping records.", "او در نگه‌داری سوابق بسیار دقیق است.")
            ),
            synonyms = listOf("thorough", "precise", "careful"),
            antonyms = listOf("careless", "sloppy"),
            collocations = listOf("meticulous planning", "meticulous attention to detail"),
            source = "A colleague at work"
        ) to listOf("adjective"),

        card(
            deckId = deckId,
            word = "give up",
            partOfSpeech = "phrasal verb",
            phonetic = "/ɡɪv ʌp/",
            meaning = "تسلیم شدن، دست کشیدن",
            englishDefinition = "To stop trying to do something.",
            examples = listOf(
                Example("Don't give up when things get hard.", "وقتی شرایط سخت می‌شود، دست نکش.")
            ),
            synonyms = listOf("quit", "surrender"),
            antonyms = listOf("persist", "persevere"),
            collocations = listOf("give up smoking", "never give up"),
            notes = "یک فعل چندبخشی (phrasal verb) است.",
            source = "A song lyric"
        ) to listOf("phrasal-verb", "verb"),

        card(
            deckId = deckId,
            word = "break the ice",
            partOfSpeech = "idiom",
            phonetic = "/breɪk ðə aɪs/",
            meaning = "یخ مجلس را شکستن، سر صحبت را باز کردن",
            englishDefinition = "To say or do something to relieve tension and start a conversation.",
            examples = listOf(
                Example("He told a joke to break the ice.", "او برای باز کردن سر صحبت جوکی گفت.")
            ),
            collocations = listOf("break the ice at a party"),
            notes = "یک اصطلاح (idiom) است؛ نمونه‌ای از پشتیبانی اپ از عبارت‌ها.",
            source = "A movie"
        ) to listOf("idiom", "phrase"),

        card(
            deckId = deckId,
            word = "endeavor",
            partOfSpeech = "noun",
            phonetic = "/ɪnˈdevər/",
            pronunciationHint = "in-DEV-er",
            meaning = "تلاش، کوشش",
            englishDefinition = "A serious and determined attempt or effort.",
            examples = listOf(
                Example("Space exploration is a costly endeavor.", "اکتشاف فضا تلاشی پرهزینه است.")
            ),
            synonyms = listOf("effort", "attempt", "venture"),
            collocations = listOf("human endeavor", "worthwhile endeavor"),
            source = "A documentary"
        ) to listOf("noun")
    )

    private fun card(
        deckId: Long,
        word: String,
        partOfSpeech: String,
        phonetic: String,
        meaning: String,
        englishDefinition: String,
        examples: List<Example>,
        pronunciationHint: String = "",
        synonyms: List<String> = emptyList(),
        antonyms: List<String> = emptyList(),
        collocations: List<String> = emptyList(),
        notes: String = "",
        source: String = ""
    ): CardEntity = CardEntity(
        deckId = deckId,
        word = word,
        partOfSpeech = partOfSpeech,
        phonetic = phonetic,
        pronunciationHint = pronunciationHint,
        meaning = meaning,
        englishDefinition = englishDefinition,
        examples = examples,
        synonyms = synonyms,
        antonyms = antonyms,
        collocations = collocations,
        notes = notes,
        source = source,
        // createdAt / updatedAt are overwritten by CardRepository.addCard.
        createdAt = 0L,
        updatedAt = 0L
    )
}
