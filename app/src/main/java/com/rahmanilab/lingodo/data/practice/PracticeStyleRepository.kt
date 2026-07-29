package com.rahmanilab.lingodo.data.practice

import com.rahmanilab.lingodo.data.preferences.SettingsRepository
import com.rahmanilab.lingodo.domain.practice.PracticeStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/** Manages built-in and user-defined Smart Practice styles and the active selection. */
class PracticeStyleRepository(private val settings: SettingsRepository) {

    private val json = Json { ignoreUnknownKeys = true }

    /** Built-in presets (with any user edits applied) followed by the user's own custom styles. */
    val styles: Flow<List<PracticeStyle>> =
        settings.customPracticeStylesJson.map { merge(decode(it)) }

    val activeStyleId: Flow<String> =
        settings.activePracticeStyleId.map { it.ifBlank { PracticeStyle.DEFAULT_ID } }

    suspend fun activeStyle(): PracticeStyle {
        val id = settings.currentActivePracticeStyleId().ifBlank { PracticeStyle.DEFAULT_ID }
        return allStyles().firstOrNull { it.id == id } ?: PracticeStyle.default()
    }

    suspend fun setActive(id: String) = settings.setActivePracticeStyleId(id)

    suspend fun saveCustom(style: PracticeStyle) {
        val id = style.id.ifBlank { "custom_${UUID.randomUUID()}" }
        val toSave = style.copy(id = id, builtIn = false)
        val custom = decode(settings.currentCustomPracticeStylesJson()).toMutableList()
        val index = custom.indexOfFirst { it.id == id }
        if (index >= 0) custom[index] = toSave else custom.add(toSave)
        settings.setCustomPracticeStylesJson(json.encodeToString(custom))
    }

    suspend fun deleteCustom(id: String) {
        val custom = decode(settings.currentCustomPracticeStylesJson()).filterNot { it.id == id }
        settings.setCustomPracticeStylesJson(json.encodeToString(custom))
        if (settings.currentActivePracticeStyleId() == id) {
            settings.setActivePracticeStyleId(PracticeStyle.DEFAULT_ID)
        }
    }

    private suspend fun allStyles(): List<PracticeStyle> =
        merge(decode(settings.currentCustomPracticeStylesJson()))

    /**
     * Presets first — replaced in place by a stored style that shares their id (a user edit) — then
     * any purely custom styles. A stored entry with a preset id therefore overrides rather than
     * duplicating it, and deleting that entry reverts the preset to its built-in default.
     */
    private fun merge(custom: List<PracticeStyle>): List<PracticeStyle> {
        val overrides = custom.associateBy { it.id }
        val presets = PracticeStyle.presets.map { overrides[it.id] ?: it }
        val extras = custom.filterNot { c -> PracticeStyle.presets.any { it.id == c.id } }
        return presets + extras
    }

    private fun decode(raw: String): List<PracticeStyle> =
        if (raw.isBlank()) emptyList()
        else runCatching { json.decodeFromString<List<PracticeStyle>>(raw) }.getOrDefault(emptyList())
}
