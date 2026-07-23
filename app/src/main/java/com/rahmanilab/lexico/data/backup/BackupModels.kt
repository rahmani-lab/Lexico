package com.rahmanilab.lexico.data.backup

import com.rahmanilab.lexico.data.local.entity.CardEntity
import com.rahmanilab.lexico.data.local.entity.CardScheduleEntity
import com.rahmanilab.lexico.data.local.entity.CardTagCrossRef
import com.rahmanilab.lexico.data.local.entity.DeckEntity
import com.rahmanilab.lexico.data.local.entity.ReviewLogEntity
import com.rahmanilab.lexico.data.local.entity.TagEntity
import com.rahmanilab.lexico.domain.model.Example
import kotlinx.serialization.Serializable

/**
 * A single card in a shareable export (CSV/JSON). Decks and tags are referenced by name so the file
 * is portable between installs and easy to hand-edit.
 */
@Serializable
data class CardExportDto(
    val deck: String = "",
    val word: String = "",
    val partOfSpeech: String = "",
    val phonetic: String = "",
    val pronunciationHint: String = "",
    val meaning: String = "",
    val englishDefinition: String = "",
    val examples: List<Example> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val collocations: List<String> = emptyList(),
    val notes: String = "",
    val source: String = "",
    val imageUri: String? = null,
    val languageCode: String = "en-US",
    val tags: List<String> = emptyList()
)

/** Top-level container for a card export. */
@Serializable
data class CardsExport(
    val version: Int = 1,
    val exportedAt: Long = 0L,
    val cards: List<CardExportDto> = emptyList()
)

/**
 * A complete backup snapshot — every table, including schedules and the full review history — so a
 * device transfer or cloud backup loses nothing (and keeps FSRS-ready learning data).
 */
@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = 0L,
    val decks: List<DeckEntity> = emptyList(),
    val cards: List<CardEntity> = emptyList(),
    val schedules: List<CardScheduleEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val cardTags: List<CardTagCrossRef> = emptyList(),
    val reviewLogs: List<ReviewLogEntity> = emptyList()
)

/** Outcome of an import or restore, surfaced to the user as a short message. */
data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val message: String,
    val success: Boolean = true
)
