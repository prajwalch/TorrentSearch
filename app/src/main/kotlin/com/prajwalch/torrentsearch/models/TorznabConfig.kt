package com.prajwalch.torrentsearch.models

/** A Torznab configuration. */
data class TorznabConfig(
    val searchProviderName: String,
    val url: String,
    val apiKey: String,
    val category: Category = Category.All,
)