package com.prajwalch.torrentsearch

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class TorrentsRepository {
    /** List of built-in providers. */
    private val providers: List<Provider> = emptyList()

    /** Starts a search for the given query. */
    suspend fun search(query: String, contentType: ContentType = ContentType.All): List<Torrent> =
        coroutineScope {
            val all = providers.map { async { it.fetch(query, contentType) } }
            return@coroutineScope all.awaitAll().flatten()
        }
}

/** A results provider.
 *
 * A provider is responsible for fetching, parsing and finally returning
 * search results from a data source. A data source may be an API or a website.
 */
interface Provider {
    /**
     * Returns the rank of the provider.
     *
     * The higher the rank, the higher the priority of the provider.
     *
     * See [Rank] for more details.
     */
    fun rank() = Rank.MEDIUM

    /** Returns the content type the provider is specialized for, if any.
     *
     * If `null` is returned, the provider will be used for all content types.
     */
    fun specializedContentType(): ContentType? = null

    /** Returns the name of the provider. */
    fun name(): String

    /** Performs a search and returns the results. */
    suspend fun fetch(query: String, contentType: ContentType): List<Torrent>
}

/** The rank of a provider, indicating its priority and quality.
 *
 * A rank is a measure of the quality of a provider.
 *
 * The quality of a provider is determined by how reliable it is and how
 * efficient it is. The quality is used to prioritize the use of a provider.
 * Ranks are categorized as follows:
 *
 * ## Highest (`0-5`)
 *
 * Providers which are specialized for a specific content type and use an
 * official API of their data source fall into this range of ranks.
 *
 * Providers within this rank will be tried first and will be retried once
 * (if they fail) before using any other provider/s.
 *
 * **NOTE**: An API can be a REST API, RSS feed, etc. but they must be official
 * and reliable to use.
 *
 * ## High (`6-40`)
 *
 * Providers which do not have a specific content type and use an official API
 * of their data source fall into this range of ranks.
 *
 * Providers in this rank are attempted if those in the highest rank either fail
 * or do not yield sufficient results.
 *
 * ## Medium (`41-90`)
 *
 * Providers which manually scrape results from a website and may or may not
 * be specialized for a specific content type fall into this range of ranks.
 *
 * ## Lowest (`91-100`)
 *
 * Providers which are considered to be as a backup to the ones in the highest
 * rank fall into this range of ranks.
 *
 * Providers in this rank may or may not be specialized for a specific content
 * type and may or may not use an official API of their data source. This rank
 * is the "catch-all" for providers that do not fit into any of the other ranks.
 */
data class Rank(private val rank: UInt) {
    companion object {
        /** Default rank for the highest category */
        val HIGHEST = Rank(1u)

        /** Default rank for the high category. */
        val HIGH = Rank(6u)

        /** Default rank for the medium category. */
        val MEDIUM = Rank(41u)

        /** Default rank for the lowest category. */
        val LOWEST = Rank(91u)

        /** Creates a new rank in the highest category */
        fun highest(value: UInt) = Rank(value.coerceAtMost(5u))

        /** Creates a new rank in the high category */
        fun high(value: UInt) = Rank(value.coerceIn(6u, 40u))

        /** Creates a new rank in the medium category */
        fun medium(value: UInt) = Rank(value.coerceIn(41u, 90u))

        /** Creates a new rank in the lowest category */
        fun lowest(value: UInt) = Rank(value.coerceIn(91u, 100u))
    }
}

/** A type of content to search for. */
enum class ContentType {
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