package com.prajwalch.torrentsearch.models

typealias SearchHistoryId = Long

data class SearchHistory(
    val id: SearchHistoryId = 0L,
    val query: String,
)