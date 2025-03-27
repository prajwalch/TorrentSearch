package com.prajwalch.torrentsearch.data

import com.prajwalch.torrentsearch.HttpClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class TorrentsRepository {
    /** List of built-in providers. */
    private val providers: List<Provider> = emptyList()

    /** The main client for making request. */
    private val httpClient = HttpClient()

    /** Starts a search for the given query. */
    suspend fun search(query: String, contentType: ContentType = ContentType.All): List<Torrent> =
        coroutineScope {
            val context = SearchContext(contentType, httpClient)
            val all = providers.map { async { it.fetch(query, context) } }
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
    suspend fun fetch(query: String, context: SearchContext): List<Torrent>
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

/** The search context. */
data class SearchContext(
    val contentType: ContentType,
    val httpClient: HttpClient
)

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

/** Metadata information of a torrent */
data class Torrent(
    val name: String = "UNKNOWN",
    val hash: String = "0000",
    val size: FileSize = FileSize(),
    val seeds: UInt = 0U,
    val peers: UInt = 0U
) {
    /** Constructs and return the magnet URL of this torrent */
    fun magnetURL(): String {
        // List of trackers to use in the magnet link.
        //
        // Taken from:
        //   https://yts.mx/api
        //   https://github.com/qbittorrent/search-plugins/blob/master/nova3/engines/torrentscsv.py#L41
        val trackers = listOf(
            "udp://tracker.internetwarriors.net:1337/announce",
            "udp://tracker.opentrackr.org:1337/announce",
            "udp://tracker.openbittorrent.com:6969/announce",
            "udp://tracker.torrent.eu.org:451/announce",
            "udp://tracker.tiny-vps.com:6969/announce",
            "udp://tracker.dler.org:6969/announce",
            "udp://tracker.coppersurfer.tk:6969",
            "udp://tracker.leechers-paradise.org:6969",
            "udp://p4p.arenabg.ch:1337/announce",
            "udp://www.torrent.eu.org:451/announce",
            "udp://retracker.lanta-net.ru:2710/announce",
            "udp://open.stealth.si:80/announce",
            "udp://exodus.desync.com:6969/announce",
            "udp://9.rarbg.me:2970/announce",
            "udp://ipv4.tracker.harry.lu:80/announce",
            "udp://torrent.gresille.org:80/announce",
        )
        // For example: &tr=<trackerurl>&tr=<tracker2url>
        val formattedTrackers = trackers.joinToString(
            separator = "",
            prefix = "&tr="
        )
        return "magnet:?xt:urn:btih:${this.hash}$formattedTrackers"
    }
}

data class FileSize(val value: Float = 0.0F, val unit: String = "B") {
    companion object {
        const val KB: Float = 1024.0F
        const val MB: Float = KB * 1024.0F
        const val GB: Float = MB * 1024.0F
        const val TB: Float = GB * 1024.0F
        const val PB: Float = TB * 1024.0F

        fun fromBytes(bytes: Float): FileSize = when {
            bytes >= PB -> FileSize(bytes / PB, "PB")
            bytes >= TB -> FileSize(bytes / TB, "TB")
            bytes >= GB -> FileSize(bytes / GB, "GB")
            bytes >= MB -> FileSize(bytes / MB, "MB")
            bytes >= KB -> FileSize(bytes / KB, "KB")
            else -> FileSize(0.0F, "B")
        }

        fun fromString(str: String): FileSize {
            return fromBytes(str.toFloatOrNull() ?: 0.0F)
        }
    }

    override fun toString(): String {
        return "$value $unit"
    }
}