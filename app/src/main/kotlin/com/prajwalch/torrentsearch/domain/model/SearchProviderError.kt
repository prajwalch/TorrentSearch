package com.prajwalch.torrentsearch.domain.model

/**
 * Represents an error that happened when launching a search provider.
 */
data class SearchProviderError(
    /**
     * Name of the crashed search provider.
     */
    val providerName: String,
    /**
     * Search provider base URL.
     */
    val providerUrl: String,
    /**
     * Error message.
     */
    val message: String?,
    /**
     * What caused the error?.
     */
    val cause: Throwable?,
)