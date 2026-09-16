package com.prajwalch.torrentsearch.provider

import com.prajwalch.torrentsearch.domain.model.MagnetUri

/**
 * A [SearchProvider] capable of resolving magnet URIs.
 *
 * Must be implemented by providers that return torrents with
 * [MagnetUri.RequiresFetch].
 */
interface MagnetUriProvider : SearchProvider {
    /**
     * Fetches the magnet URI from the [sourceUrl].
     */
    suspend fun getMagnetUri(sourceUrl: String): String
}