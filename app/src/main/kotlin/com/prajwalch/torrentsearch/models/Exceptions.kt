package com.prajwalch.torrentsearch.models

import com.prajwalch.torrentsearch.providers.SearchProviderId

/** Base exception for all TorrentSearch exceptions. */
sealed class TorrentSearchException(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Base exception for search provider related exceptions.
 *
 * @param id Search provider ID
 * @param name Search provider name
 * @param url Search provider base URL
 */
class SearchProviderException(
    val id: SearchProviderId,
    val name: String,
    val url: String,
    message: String? = null,
    cause: Throwable? = null,
) : TorrentSearchException(message, cause) {
    override fun toString(): String {
        return buildString {
            append("SearchProviderException($id = $id, name = $name, url = $url)")

            if (message != null) {
                append(": ")
                append(message)
            }
        }
    }
}