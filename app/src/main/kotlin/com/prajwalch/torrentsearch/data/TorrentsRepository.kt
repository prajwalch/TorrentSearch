package com.prajwalch.torrentsearch.data

import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.providers.Eztv
import com.prajwalch.torrentsearch.providers.ThePirateBay
import com.prajwalch.torrentsearch.providers.TheRarBg
import com.prajwalch.torrentsearch.providers.Yts

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class TorrentsRepository(private val httpClient: HttpClient) {
    /** List of built-in providers. */
    private val searchProviders: List<SearchProvider> = listOf(
        Eztv(),
        ThePirateBay(),
        TheRarBg(),
        Yts(),
    )

    /** Starts a search for the given query. */
    suspend fun search(query: String, category: Category): List<Torrent> = coroutineScope {
        val query = query.replace(oldChar = ' ', newChar = '+')
        val context = SearchContext(category, httpClient)

        searchProviders
            .filter {
                (category == Category.All)
                        || (it.specializedCategory() == Category.All)
                        || (category == it.specializedCategory())
            }
            .map { async { it.search(query, context) } }
            .awaitAll()
            .flatten()
            .sortedByDescending { it.seeds }
    }

    /** Returns `true` is the internet is available to perform a `search()`. */
    suspend fun isInternetAvailable(): Boolean = coroutineScope { httpClient.isInternetAvailable() }
}