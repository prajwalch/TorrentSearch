package com.prajwalch.torrentsearch.provider

import com.prajwalch.torrentsearch.domain.model.TorrentDetails

/**
 * A [SearchProvider] that can provide the torrent details.
 */
interface TorrentDetailsProvider : SearchProvider {
    /**
     * List of alternate details page domains.
     *
     * When matching a details provider, the [url] is checked first,
     * then these alternate domains, then the provider [name] as a last resort.
     */
    val alternateUrlDomains: List<String> get() = emptyList()

    /**
     * Fetches the [TorrentDetails] for a torrent from the [detailsPageUrl].
     *
     * Return `null` if the details could not be fetched or parsed.
     */
    suspend fun getDetails(detailsPageUrl: String): TorrentDetails?
}