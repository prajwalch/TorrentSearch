package com.prajwalch.torrentsearch.domain.model

/** A Torznab configuration. */
data class TorznabConfig(
    val id: String,
    val searchProviderName: String,
    val url: String,
    val apiKey: String,
    val supportedCategories: Set<Category> = emptySet(),
)