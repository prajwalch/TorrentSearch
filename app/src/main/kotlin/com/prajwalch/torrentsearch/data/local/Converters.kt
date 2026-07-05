package com.prajwalch.torrentsearch.data.local

import androidx.room.TypeConverter
import com.prajwalch.torrentsearch.domain.model.Category

object Converters {
    @TypeConverter
    fun categorySetToString(categories: Set<Category>): String {
        return categories.joinToString(separator = ",") { it.name }
    }

    @TypeConverter
    fun stringToCategorySet(raw: String): Set<Category> {
        return if (raw.isBlank()) {
            emptySet()
        } else {
            raw.split(",")
                .mapNotNull { runCatching { Category.valueOf(it) }.getOrNull() }
                .toSet()
        }
    }
}