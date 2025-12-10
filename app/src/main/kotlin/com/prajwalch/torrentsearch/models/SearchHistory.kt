package com.prajwalch.torrentsearch.models

@JvmInline
value class SearchHistoryId(val value: Int)

data class SearchHistory(
    val id: SearchHistoryId = SearchHistoryId(0),
    val query: String,
)