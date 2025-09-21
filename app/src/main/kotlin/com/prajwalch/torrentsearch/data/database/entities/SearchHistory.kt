package com.prajwalch.torrentsearch.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

typealias SearchHistoryId = Long

@Entity(
    tableName = "search_history",
    indices = [Index("query", unique = true)],
)
data class SearchHistory(
    @PrimaryKey(autoGenerate = true)
    val id: SearchHistoryId = 0,
    val query: String,
)