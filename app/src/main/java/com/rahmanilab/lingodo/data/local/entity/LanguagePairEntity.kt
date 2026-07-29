package com.rahmanilab.lingodo.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A study workspace: a pair of a source (native/explanation) language and a target (study) language,
 * e.g. source "fa" / target "de" for a Persian speaker learning German. Decks belong to a pair, and
 * the app filters everything by the currently active pair.
 */
@Serializable
@Entity(
    tableName = "language_pairs",
    indices = [Index(value = ["sourceCode", "targetCode"], unique = true)]
)
data class LanguagePairEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** ISO code of the native/explanation language. */
    val sourceCode: String,
    /** ISO code of the language being studied. */
    val targetCode: String,
    val createdAt: Long
)
