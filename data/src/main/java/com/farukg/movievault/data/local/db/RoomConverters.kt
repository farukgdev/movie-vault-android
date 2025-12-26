package com.farukg.movievault.data.local.db

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class RoomConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun genresToJson(value: List<String>): String =
        json.encodeToString(ListSerializer(String.serializer()), value)

    @TypeConverter
    fun jsonToGenres(value: String): List<String> =
        runCatching { json.decodeFromString(ListSerializer(String.serializer()), value) }
            .getOrElse { emptyList() }
}
