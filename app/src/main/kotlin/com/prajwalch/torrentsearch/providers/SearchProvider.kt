package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.network.HttpClient

/** Unique identifier of the provider. */
typealias SearchProviderId = String

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
    val specializedCategory: Category = Category.All,
    /** Safety status of the search provider */
    val safetyStatus: SearchProviderSafetyStatus,
)

/** How safe is the search provider?. */
sealed class SearchProviderSafetyStatus {
    /** Search provider is safe to use. */
    object Safe : SearchProviderSafetyStatus()

    /** Search provider is not safe and requires special care to use it. */
    data class Unsafe(val reason: String) : SearchProviderSafetyStatus()
}

/** The search context. */
data class SearchContext(
    val category: Category,
    val httpClient: HttpClient,
)