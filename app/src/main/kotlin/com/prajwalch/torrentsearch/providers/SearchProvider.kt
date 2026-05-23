package com.prajwalch.torrentsearch.providers

import androidx.annotation.StringRes

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent
import com.prajwalch.torrentsearch.domain.model.TorrentDetails
import com.prajwalch.torrentsearch.network.HttpClient

/** Unique identifier of the provider. */
typealias SearchProviderId = String

/**
 * A search provider is responsible for initiating the search for the given
 * query, parsing the response and then returning structured result.
 */
interface SearchProvider {
    /** Unique ID of the search provider. */
    val id: SearchProviderId

    /** Name of the search provider. */
    val name: String

    /** URL of the search provider. */
    val url: String

    /**
     * Categories supported by the search provider.
     *
     * Include a category if the upstream server either accepts it as a search
     * parameter, or classifies returned results with it even without supporting
     * category-based search.
     */
    val supportedCategories: Set<Category> get() = emptySet()

    /** Safety status of the search provider */
    val safetyStatus: SearchProviderSafetyStatus

    /** Default state of the provider. */
    val enabledByDefault: Boolean

    /** Type of search provider. */
    val type: SearchProviderType get() = SearchProviderType.Builtin

    /** Performs a search and returns the results. */
    suspend fun search(query: String, context: SearchContext): List<Torrent>
}

/**
 * A [SearchProvider] that also supports torrent details fetching.
 */
interface TorrentDetailsProvider : SearchProvider {
    /**
     * List of alternate domains for details page.
     */
    val alternateUrlDomains: List<String> get() = emptyList()

    /**
     * Extracts and returns [TorrentDetails] from the given URL if successful,
     * otherwise returns `null`.
     */
    suspend fun getDetails(detailsPageUrl: String): TorrentDetails?
}

/**
 * A [SearchProvider] that also provides newly added torrents.
 */
interface LatestTorrentsProvider : SearchProvider {
    suspend fun getLastestTorrents(category: Category = Category.All): List<Torrent>
}

/**
 * A [SearchProvider] that also provides top (or trending) torrents.
 */
interface TopTorrentsProvider : SearchProvider {
    suspend fun getTopTorrents(category: Category = Category.All): List<Torrent>
}

/** How safe is the search provider?. */
sealed class SearchProviderSafetyStatus {
    /** Search provider is safe to use. */
    object Safe : SearchProviderSafetyStatus()

    /** Search provider is not safe and requires special care to use it. */
    data class Unsafe(@field:StringRes val reason: Int) : SearchProviderSafetyStatus()

    /** Returns `true` if the status is [SearchProviderSafetyStatus.Unsafe]. */
    fun isUnsafe(): Boolean = this is Unsafe
}

/** Specifies the type of search provider. */
enum class SearchProviderType {
    /** Search provider is built-in. */
    Builtin,

    /**
     * Search provider is externally added Torznab API compatible search
     * provider.
     */
    Torznab
}

/** The search context. */
data class SearchContext(
    val category: Category,
    val httpClient: HttpClient,
)