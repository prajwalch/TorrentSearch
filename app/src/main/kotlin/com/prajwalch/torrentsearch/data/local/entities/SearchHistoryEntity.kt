package com.prajwalch.torrentsearch.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

import com.prajwalch.torrentsearch.domain.models.SearchHistory

@Entity(
    tableName = "search_history",
    indices = [Index("query", unique = true)],
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val query: String,
)

fun SearchHistoryEntity.toDomain() =
    SearchHistory(id = this.id, query = this.query)

fun SearchHistory.toEntity() =
    SearchHistoryEntity(id = this.id, query = this.query)

fun List<SearchHistoryEntity>.toDomain() =
    this.map { it.toDomain() }