package com.rahmanilab.lingodo.data.backup

import androidx.room.withTransaction
import com.rahmanilab.lingodo.data.local.LingoDoDatabase
import com.rahmanilab.lingodo.data.local.entity.CardEntity
import com.rahmanilab.lingodo.data.local.entity.DeckEntity
import com.rahmanilab.lingodo.data.local.entity.LanguagePairEntity
import com.rahmanilab.lingodo.data.local.relation.CardWithDetails
import com.rahmanilab.lingodo.data.repository.CardRepository
import com.rahmanilab.lingodo.data.repository.DeckRepository
import com.rahmanilab.lingodo.domain.model.Example
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Imports and exports card data and full backups.
 *
 * The UI drives all file access through the Storage Access Framework, so exports can be written to
 * (and imports read from) any location the user picks — including cloud providers such as Google
 * Drive or Dropbox, which is how LingoDo offers "cloud backup" without bundling any account SDK.
 */
class ImportExportRepository(
    private val db: LingoDoDatabase,
    private val deckRepository: DeckRepository,
    private val cardRepository: CardRepository,
    private val settingsRepository: com.rahmanilab.lingodo.data.preferences.SettingsRepository
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ---------------------------------------------------------------- exports

    suspend fun exportCardsJson(): String {
        val decks = db.deckDao().getAll().associateBy { it.id }
        val cards = db.cardDao().getAllWithDetails()
        val dtos = cards.map { it.toExportDto(decks[it.card.deckId]?.name.orEmpty()) }
        return json.encodeToString(CardsExport(exportedAt = System.currentTimeMillis(), cards = dtos))
    }

    suspend fun exportCardsCsv(): String {
        val decks = db.deckDao().getAll().associateBy { it.id }
        val cards = db.cardDao().getAllWithDetails()
        return Csv.export(cards, decks)
    }

    suspend fun exportBackupJson(): String {
        val backup = BackupData(
            exportedAt = System.currentTimeMillis(),
            languagePairs = db.languagePairDao().getAll(),
            decks = db.deckDao().getAll(),
            cards = db.cardDao().getAllCards(),
            schedules = db.cardScheduleDao().getAll(),
            tags = db.tagDao().getAllTags(),
            cardTags = db.tagDao().getAllCrossRefs(),
            reviewLogs = db.reviewLogDao().getAll()
        )
        return json.encodeToString(backup)
    }

    // ---------------------------------------------------------------- imports

    suspend fun importJson(text: String): ImportResult {
        val cards = runCatching { json.decodeFromString<CardsExport>(text).cards }
            .recoverCatching { json.decodeFromString<List<CardExportDto>>(text) }
            .getOrNull()
            ?: return ImportResult(0, 0, "Could not parse the JSON file.", success = false)
        return addCards(cards)
    }

    suspend fun importCsv(text: String): ImportResult {
        val cards = runCatching { Csv.parse(text) }.getOrNull()
            ?: return ImportResult(0, 0, "Could not parse the CSV file.", success = false)
        return addCards(cards)
    }

    /**
     * Imports Anki's "Notes in Plain Text" export: tab-separated `front <tab> back [<tab> tags]`,
     * with `#`-prefixed header lines ignored and basic HTML stripped from fields.
     */
    suspend fun importAnkiTsv(text: String): ImportResult {
        val cards = text.lineSequence()
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val cols = line.split('\t')
                val front = stripHtml(cols.getOrElse(0) { "" })
                if (front.isBlank()) return@mapNotNull null
                CardExportDto(
                    deck = "Anki Import",
                    word = front,
                    meaning = stripHtml(cols.getOrElse(1) { "" }),
                    tags = cols.getOrElse(2) { "" }
                        .split(' ', ',')
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                )
            }
            .toList()
        return addCards(cards)
    }

    private fun stripHtml(value: String): String = value
        .replace(Regex("<[^>]*>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&#39;", "'")
        .replace("&quot;", "\"")
        .replace(Regex("\\s+"), " ")
        .trim()

    private suspend fun addCards(dtos: List<CardExportDto>): ImportResult {
        var imported = 0
        var skipped = 0
        // Import into the active language pair; match existing decks by name within that pair.
        val pairId = settingsRepository.currentActivePairId()
        val decksByName = db.deckDao().getAll()
            .filter { it.languagePairId == pairId }
            .associateBy { it.name.lowercase() }
            .toMutableMap()

        for (dto in dtos) {
            if (dto.word.isBlank()) {
                skipped++
                continue
            }
            val deckName = dto.deck.ifBlank { "Imported" }
            val deckId = decksByName[deckName.lowercase()]?.id ?: run {
                val id = deckRepository.createDeck(deckName, "", pairId)
                deckRepository.getDeck(id)?.let { decksByName[deckName.lowercase()] = it }
                id
            }
            if (cardRepository.countWithWordInDeck(deckId, dto.word) > 0) {
                skipped++ // duplicate detection
                continue
            }
            cardRepository.addCard(dto.toEntity(deckId), dto.tags)
            imported++
        }
        return ImportResult(imported, skipped, "Imported $imported card(s); skipped $skipped.")
    }

    // ---------------------------------------------------------------- restore

    /**
     * Non-destructive counterpart to [restoreBackupJson]: appends a backup's contents to the current
     * database instead of replacing it. Decks are matched by name within the active language pair
     * (created when missing) and cards whose word already exists in the target deck are skipped, so
     * merging the same file twice never duplicates anything. Each imported card keeps its own
     * learning schedule, with ids remapped to the newly inserted rows.
     */
    suspend fun mergeBackupJson(text: String): ImportResult {
        val backup = runCatching { json.decodeFromString<BackupData>(text) }.getOrNull()
            ?: return ImportResult(0, 0, "Could not parse the backup file.", success = false)

        val pairId = settingsRepository.currentActivePairId()
        val decksById = backup.decks.associateBy { it.id }
        val schedulesByCard = backup.schedules.associateBy { it.cardId }
        val tagNamesById = backup.tags.associate { it.id to it.name }
        val tagNamesByCard: Map<Long, List<String>> = backup.cardTags
            .groupBy { it.cardId }
            .mapValues { (_, refs) -> refs.mapNotNull { tagNamesById[it.tagId] } }

        val existingDecks = db.deckDao().getAll()
            .filter { it.languagePairId == pairId }
            .associateBy { it.name.lowercase() }
            .toMutableMap()

        var imported = 0
        var skipped = 0
        for (card in backup.cards) {
            if (card.word.isBlank()) {
                skipped++
                continue
            }
            val deckName = decksById[card.deckId]?.name?.takeIf { it.isNotBlank() } ?: "Imported"
            val deckId = existingDecks[deckName.lowercase()]?.id ?: run {
                val id = deckRepository.createDeck(deckName, "", pairId)
                deckRepository.getDeck(id)?.let { existingDecks[deckName.lowercase()] = it }
                id
            }
            if (cardRepository.countWithWordInDeck(deckId, card.word) > 0) {
                skipped++ // already present — merge never overwrites
                continue
            }
            val newId = cardRepository.addCard(
                card.copy(id = 0, deckId = deckId),
                tagNamesByCard[card.id].orEmpty()
            )
            // Carry the card's progress across, pointing the schedule at the new row.
            schedulesByCard[card.id]?.let { db.cardScheduleDao().upsert(it.copy(cardId = newId)) }
            imported++
        }
        return ImportResult(imported, skipped, "Merged $imported card(s); skipped $skipped duplicate(s).")
    }

    /** Wipes the database and replaces it with the contents of a full backup file. */
    suspend fun restoreBackupJson(text: String): ImportResult {
        val backup = runCatching { json.decodeFromString<BackupData>(text) }.getOrNull()
            ?: return ImportResult(0, 0, "Could not parse the backup file.", success = false)

        // Guarantee at least one language pair so restored decks always have a valid workspace.
        val pairs = backup.languagePairs.ifEmpty {
            listOf(LanguagePairEntity(id = 1, sourceCode = "fa", targetCode = "en", createdAt = System.currentTimeMillis()))
        }
        db.clearAllTables()
        db.withTransaction {
            db.languagePairDao().insertAll(pairs)
            db.deckDao().insertAll(backup.decks)
            db.cardDao().insertAll(backup.cards)
            db.tagDao().insertAllTags(backup.tags)
            db.tagDao().insertAllCrossRefs(backup.cardTags)
            db.cardScheduleDao().insertAll(backup.schedules)
            db.reviewLogDao().insertAll(backup.reviewLogs)
        }
        settingsRepository.setActivePairId(pairs.first().id)
        return ImportResult(backup.cards.size, 0, "Restored ${backup.cards.size} card(s) from backup.")
    }
}

private fun CardWithDetails.toExportDto(deckName: String): CardExportDto = CardExportDto(
    deck = deckName,
    word = card.word,
    partOfSpeech = card.partOfSpeech,
    phonetic = card.phonetic,
    pronunciationHint = card.pronunciationHint,
    meaning = card.meaning,
    englishDefinition = card.englishDefinition,
    examples = card.examples,
    synonyms = card.synonyms,
    antonyms = card.antonyms,
    collocations = card.collocations,
    wordForms = card.wordForms,
    notes = card.notes,
    source = card.source,
    imageUri = card.imageUri,
    languageCode = card.languageCode,
    tags = tags.map { it.name }
)

private fun CardExportDto.toEntity(deckId: Long): CardEntity = CardEntity(
    deckId = deckId,
    word = word.trim(),
    partOfSpeech = partOfSpeech,
    phonetic = phonetic,
    pronunciationHint = pronunciationHint,
    meaning = meaning,
    englishDefinition = englishDefinition,
    examples = examples,
    synonyms = synonyms,
    antonyms = antonyms,
    collocations = collocations,
    wordForms = wordForms,
    notes = notes,
    source = source,
    imageUri = imageUri,
    languageCode = languageCode,
    createdAt = 0L,
    updatedAt = 0L
)

/** Minimal RFC 4180–style CSV reader/writer for card data. */
private object Csv {

    private val HEADER = listOf(
        "deck", "word", "partOfSpeech", "phonetic", "pronunciationHint", "meaning",
        "englishDefinition", "examples", "synonyms", "antonyms", "collocations", "notes",
        "source", "tags"
    )

    fun export(cards: List<CardWithDetails>, decks: Map<Long, DeckEntity>): String {
        val sb = StringBuilder()
        sb.append(HEADER.joinToString(",") { escape(it) }).append('\n')
        for (cwd in cards) {
            val c = cwd.card
            val row = listOf(
                decks[c.deckId]?.name.orEmpty(),
                c.word,
                c.partOfSpeech,
                c.phonetic,
                c.pronunciationHint,
                c.meaning,
                c.englishDefinition,
                c.examples.joinToString(" | ") { "${it.text} :: ${it.translation}" },
                c.synonyms.joinToString("; "),
                c.antonyms.joinToString("; "),
                c.collocations.joinToString("; "),
                c.notes,
                c.source,
                cwd.tags.joinToString("; ") { it.name }
            )
            sb.append(row.joinToString(",") { escape(it) }).append('\n')
        }
        return sb.toString()
    }

    fun parse(text: String): List<CardExportDto> {
        val rows = parseRows(text)
        if (rows.isEmpty()) return emptyList()
        val header = rows.first().map { it.trim().lowercase() }
        fun index(name: String) = header.indexOf(name.lowercase())

        val di = index("deck"); val wi = index("word"); val pi = index("partOfSpeech")
        val phi = index("phonetic"); val hi = index("pronunciationHint"); val mi = index("meaning")
        val defi = index("englishDefinition"); val exi = index("examples"); val syi = index("synonyms")
        val ai = index("antonyms"); val ci = index("collocations"); val ni = index("notes")
        val si = index("source"); val ti = index("tags")

        return rows.drop(1).mapNotNull { cols ->
            fun col(i: Int) = if (i in cols.indices) cols[i] else ""
            val word = col(wi).trim()
            if (word.isBlank()) return@mapNotNull null
            CardExportDto(
                deck = col(di).trim(),
                word = word,
                partOfSpeech = col(pi).trim(),
                phonetic = col(phi).trim(),
                pronunciationHint = col(hi).trim(),
                meaning = col(mi).trim(),
                englishDefinition = col(defi).trim(),
                examples = parseExamples(col(exi)),
                synonyms = splitList(col(syi)),
                antonyms = splitList(col(ai)),
                collocations = splitList(col(ci)),
                notes = col(ni).trim(),
                source = col(si).trim(),
                tags = splitList(col(ti))
            )
        }
    }

    private fun splitList(value: String): List<String> =
        value.split(";").map { it.trim() }.filter { it.isNotEmpty() }

    private fun parseExamples(value: String): List<Example> =
        value.split("|").mapNotNull { part ->
            val trimmed = part.trim()
            if (trimmed.isEmpty()) return@mapNotNull null
            val bits = trimmed.split("::", limit = 2)
            Example(bits.getOrElse(0) { "" }.trim(), bits.getOrElse(1) { "" }.trim())
        }

    private fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }

    private fun parseRows(text: String): List<List<String>> {
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        var field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < normalized.length) {
            val ch = normalized[i]
            when {
                inQuotes -> when {
                    ch == '"' && i + 1 < normalized.length && normalized[i + 1] == '"' -> {
                        field.append('"'); i++
                    }
                    ch == '"' -> inQuotes = false
                    else -> field.append(ch)
                }
                ch == '"' -> inQuotes = true
                ch == ',' -> { row.add(field.toString()); field = StringBuilder() }
                ch == '\n' -> { row.add(field.toString()); rows.add(row); row = mutableListOf(); field = StringBuilder() }
                else -> field.append(ch)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }
        return rows.filter { cols -> cols.any { it.isNotBlank() } }
    }
}
