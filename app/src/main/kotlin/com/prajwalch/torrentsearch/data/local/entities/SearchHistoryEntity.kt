package com.prajwalch.torrentsearch.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

import com.prajwalch.torrentsearch.models.SearchHistory
import com.prajwalch.torrentsearch.models.SearchHistoryId

@Entity(
    tableName = "search_history",
    indices = [Index("query", unique = true)],
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val query: String,
)

fun SearchHistoryEntity.toDomain() =
    SearchHistory(id = SearchHistoryId(this.id), query = this.query)

fun SearchHistory.toEntity() =
    SearchHistoryEntity(id = this.id.value, query = this.query)

fun List<SearchHistoryEntity>.toDomain() =
    this.map { it.toDomain() }