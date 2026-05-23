package com.prajwalch.torrentsearch.domain.model

/**
 * Represents a response of
 * [com.prajwalch.torrentsearch.providers.TorrentDetailsProvider.getDetails].
 */
sealed interface GetTorrentDetailsResponse {
    /**
     * A successful response.
     */
    data class Success(val details: TorrentDetails) : GetTorrentDetailsResponse

    /**
     * Given URL is not supported.
     */
    data object UnsupportedUrl : GetTorrentDetailsResponse

    /**
     * Details couldn't be found on the remote host.
     */
    data object Unavailable : GetTorrentDetailsResponse
}