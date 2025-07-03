package com.prajwalch.torrentsearch.data

import com.prajwalch.torrentsearch.models.Torrent
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.providers.ThePirateBay
import com.prajwalch.torrentsearch.providers.TheRarBg
import com.prajwalch.torrentsearch.providers.Yts

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class TorrentsRepository(private val httpClient: HttpClient) {
    /** List of built-in providers. */
    private val providers: List<Provider> = listOf(ThePirateBay(), TheRarBg(), Yts())

    /** Starts a search for the given query. */
    suspend fun search(query: String, category: Category): List<Torrent> = coroutineScope {
        val query = query.replace(oldChar = ' ', newChar = '+')
        val context = SearchContext(category, httpClient)

        providers
            .filter {
                (category == Category.All)
                        || (it.specializedCategory() == Category.All)
                        || (category == it.specializedCategory())
            }
            .map { async { it.fetch(query, context) } }
            .awaitAll()
            .flatten()
            .sortedByDescending { it.seeds }
    }

    /** Returns `true` is the internet is available to perform a `search()`. */
    suspend fun isInternetAvailable(): Boolean = coroutineScope { httpClient.isInternetAvailable() }
}

/** A results provider.
 *
 * A provider is responsible for fetching, parsing and finally returning
 * search results from a data source. A data source may be an API or a website.
 */
interface Provider {
    /** Returns the category the provider is specialized for. */
    fun specializedCategory() = Category.All

    /** Returns the name of the provider. */
    fun name(): String

    /** Performs a search and returns the results. */
    suspend fun fetch(query: String, context: SearchContext): List<Torrent>
}

/** The search context. */
data class SearchContext(
    val category: Category,
    val httpClient: HttpClient,
)

/** Search category. */
enum class Category {
    All,
    Anime,
    Apps,
    Books,
    Games,
    Movies,
    Music,
    Porn,
    Series,
}