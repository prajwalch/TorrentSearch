package com.prajwalch.torrentsearch.domain.model

import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.providers.SearchProviderSafetyStatus
import com.prajwalch.torrentsearch.providers.SearchProviderType

/** Search provider information. */
data class SearchProviderInfo(
    /** Unique ID of the search provider. */
    val id: SearchProviderId,
    /** Name of the search provider. */
    val name: String,
    /** URL of the search provider. */
    val url: String,
    /** URL which is used to solve Cloudflare challenge. */
    val cloudflareSolverUrl: String? = null,
    /** Categories supported by the search provider. */
    val supportedCategories: Set<Category> = emptySet(),
    /** Safety status of the search provider */
    val safetyStatus: SearchProviderSafetyStatus,
    /** Type of search provider. */
    val type: SearchProviderType,
    /** Current status of the provider protection. */
    val cloudflareProtectionStatus: CloudflareProtectionStatus,
    val isEnabled: Boolean,
)

/** Indicates the protection status of the provider. */
enum class CloudflareProtectionStatus {
    /** Not Cloudflare-protected. */
    UnProtected,

    /** Cloudflare-protected and not unlocked yet. */
    Locked,

    /** Cloudflare-protected but already unlocked. */
    Unlocked,
}