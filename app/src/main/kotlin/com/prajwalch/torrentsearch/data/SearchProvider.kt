package com.prajwalch.torrentsearch.data

import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.network.HttpClient

/**
 * A search provider is responsible for initiating the search for the given
 * query, parsing the response and then returning structured result.
 */
interface SearchProvider {
    /** Returns the category the provider is specialized for. */
    fun specializedCategory() = Category.All

    /** Performs a search and returns the results. */
    suspend fun search(query: String, context: SearchContext): List<Torrent>
}

/** The search context. */
data class SearchContext(
    val category: Category,
    val httpClient: HttpClient,
)