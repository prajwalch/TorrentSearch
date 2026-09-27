package com.prajwalch.torrentsearch.domain.model

import com.prajwalch.torrentsearch.provider.SearchProvider
import com.prajwalch.torrentsearch.provider.SearchProviderId

/**
 * Represents an error occurred when using [SearchProvider].
 */
data class SearchProviderError(
    /**
     * ID of the provider.
     */
    val providerId: SearchProviderId,
    /**
     * Name of the provider.
     */
    val providerName: String,
    /**
     * Homepage URl of the provider.
     */
    val providerUrl: String,
    /**
     * Category of the error.
     */
    val kind: Kind,
    /**
     * Exception which caused this error to happen.
     */
    val cause: Throwable?,
) {
    /**
     * Represents an error category.
     */
    enum class Kind {
        Crash,
        CloudflareChallenge,
    }

    /**
     * Indicates whether the error is retryable.
     */
    val isRetryable: Boolean
        get() = when (kind) {
            Kind.Crash -> true
            Kind.CloudflareChallenge -> false
        }
}