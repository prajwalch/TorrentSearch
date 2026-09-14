package com.prajwalch.torrentsearch.domain.model

import com.prajwalch.torrentsearch.provider.SearchProviderId

/**
 * High-level information of the search provider.
 */
data class SearchProviderInfo(
    /**
     * Unique ID of the provider.
     */
    val id: SearchProviderId,
    /**
     * Name of the provider.
     */
    val name: String,
    /**
     * Homepage URL of the provider.
     */
    val url: String,
    /**
     * The URL which should be used for solving Cloudflare challenge.
     */
    val cloudflareSolverUrl: String? = null,
    /**
     * Set of categories supported by the provider.
     */
    val supportedCategories: Set<Category> = emptySet(),
    /**
     * Safety flag of the provider.
     */
    val safetyStatus: SearchProviderSafetyStatus,
    /**
     * Origin from where the provider comes from.
     */
    val type: SearchProviderType,
    /**
     * Current protection status of the provider.
     */
    val cloudflareProtectionStatus: CloudflareProtectionStatus,
    /**
     * Indicates whether the provider is currently enabled.
     */
    val isEnabled: Boolean,
)