package com.prajwalch.torrentsearch.domain.model

/**
 * Represents a response of [com.prajwalch.torrentsearch.providers.SearchProvider.getDetails].
 */
sealed interface GetTorrentDetailsResponse {
    /**
     * A successful response.
     */
    data class Success(val details: TorrentDetails) : GetTorrentDetailsResponse

    /**
     * Request is not supported by the search provider.
     */
    data object RequestNotSupported : GetTorrentDetailsResponse

    /**
     * Details couldn't be found on the remote host.
     */
    data object DetailsNotFound : GetTorrentDetailsResponse
}