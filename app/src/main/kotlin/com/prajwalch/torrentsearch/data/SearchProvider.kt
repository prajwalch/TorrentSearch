package com.prajwalch.torrentsearch.data

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
    /** Provider id. */
    val id: SearchProviderId

    /** Name of the provider. */
    val name: String

    /** Category the provider is specialized for. */
    val specializedCategory: Category
        get() = Category.All

    /** Performs a search and returns the results. */
    suspend fun search(query: String, context: SearchContext): List<Torrent>
}

/** The search context. */
data class SearchContext(
    val category: Category,
    val httpClient: HttpClient,
)