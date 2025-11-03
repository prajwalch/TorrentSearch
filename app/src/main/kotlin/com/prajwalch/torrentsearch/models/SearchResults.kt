package com.prajwalch.torrentsearch.models

data class SearchResults(
    val successes: List<Torrent>,
    val failures: List<Throwable>,
)