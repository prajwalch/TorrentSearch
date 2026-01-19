package com.prajwalch.torrentsearch.domain.models

/** Base exception for all TorrentSearch exceptions. */
sealed class TorrentSearchException(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Base exception for search related exceptions.
 *
 * @param searchProviderName Search provider name
 * @param searchProviderUrl Search provider base URL
 */
class SearchException(
    val searchProviderName: String,
    val searchProviderUrl: String,
    message: String? = null,
    cause: Throwable? = null,
) : TorrentSearchException(message, cause) {
    override fun toString(): String {
        return buildString {
            append("SearchException")
            append(" ")
            append('[')
            append("searchProviderName=$searchProviderName")
            append(", ")
            append("searchProviderUrl=$searchProviderUrl")
            append(']')

            if (message != null) {
                append(": ")
                append(message)
            }
        }
    }
}