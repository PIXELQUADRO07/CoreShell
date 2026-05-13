package com.example.coreshell.data

import androidx.room.TypeConverter
import org.json.JSONArray

class Converters {

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        val json = JSONArray()
        list?.forEach { json.put(it) }
        return json.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            val json = JSONArray(value)
            List(json.length()) { index -> json.optString(index, "") }
                .filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
