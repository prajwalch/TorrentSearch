package com.prajwalch.torrentsearch.domain.model

/**
 * Represents an error that happened when launching a search provider.
 */
data class SearchProviderError(
    /**
     * Name of the search provider for which this error belongs.
     */
    val providerName: String,
    /**
     * Search provider base URL.
     */
    val providerUrl: String,
    /**
     * What was the reason for failure?
     */
    val failureReason: SearchProviderFailureReason,
    /**
     * What caused the error?.
     */
    val cause: Throwable?,
)

enum class SearchProviderFailureReason {
    Crash,
    CloudflareChallenge,
}