package com.prajwalch.torrentsearch.domain.model

/**
 * Response returned when requesting a torrent details.
 */
sealed interface GetTorrentDetailsResponse {
    /**
     * Request succeed.
     */
    data class Success(val details: TorrentDetails) : GetTorrentDetailsResponse

    /**
     * Request URL is not supported.
     *
     * It occurs when no any providers are found that can handle the
     * request URl.
     */
    data object UnsupportedUrl : GetTorrentDetailsResponse

    /**
     * Details not available.
     *
     * It indicates that the request URL is valid and reachable, but
     * could not find the details or parsed the page maybe due to layout
     * change.
     */
    data object Unavailable : GetTorrentDetailsResponse
}