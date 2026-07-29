package com.rahmanilab.lingodo.data.local

import androidx.room.TypeConverter
import com.rahmanilab.lingodo.domain.model.Example
import com.rahmanilab.lingodo.domain.model.WordForm
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room type converters for the structured list fields on [com.rahmanilab.lingodo.data.local.entity.CardEntity].
 * Everything is stored as compact JSON.
 */
class Converters {

    @TypeConverter
    fun fromExampleList(value: List<Example>): String = json.encodeToString(value)

    @TypeConverter
    fun toExampleList(value: String): List<Example> =
        if (value.isBlank()) emptyList() else json.decodeFromString(value)

    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else json.decodeFromString(value)

    @TypeConverter
    fun fromWordFormList(value: List<WordForm>): String = json.encodeToString(value)

    @TypeConverter
    fun toWordFormList(value: String): List<WordForm> =
        if (value.isBlank()) emptyList() else json.decodeFromString(value)

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
