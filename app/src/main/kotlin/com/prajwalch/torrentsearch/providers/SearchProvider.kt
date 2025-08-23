package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.network.HttpClient

/**
 * A search provider is responsible for initiating the search for the given
 * query, parsing the response and then returning structured result.
 */
interface SearchProvider {
    /** Search provider information. */
    val info: SearchProviderInfo

    /** Performs a search and returns the results. */
    suspend fun search(query: String, context: SearchContext): List<Torrent>
}

/** Search provider information. */
data class SearchProviderInfo(
    /** Unique ID of the search provider. */
    val id: SearchProviderId,
    /** Name of the search provider. */
    val name: String,
    /** URL of the search provider. */
    val url: String,
    /** Category in which the provider specializes. */
    val specializedCategory: Category,
    /** Safety status of the search provider */
    val safetyStatus: SearchProviderSafetyStatus,
    /** Default state of the provider. */
    val enabledByDefault: Boolean,
    /** Type of search provider. */
    val type: SearchProviderType = SearchProviderType.Builtin,
)

/** Unique identifier of the provider. */
typealias SearchProviderId = String

/** How safe is the search provider?. */
sealed class SearchProviderSafetyStatus {
    /** Search provider is safe to use. */
    object Safe : SearchProviderSafetyStatus()

    /** Search provider is not safe and requires special care to use it. */
    data class Unsafe(val reason: String) : SearchProviderSafetyStatus()

    /** Returns `true` if the status is [SearchProviderSafetyStatus.Unsafe]. */
    fun isUnsafe(): Boolean = this is Unsafe
}

/** Specifies the type of search provider. */
sealed class SearchProviderType {
    /** Search provider is built-in. */
    object Builtin : SearchProviderType()

    /**
     * Search provider is externally added Torznab API compatible search
     * provider.
     */
    data class Torznab(val apiKey: String) : SearchProviderType()
}

/** The search context. */
data class SearchContext(
    val category: Category,
    val httpClient: HttpClient,
)