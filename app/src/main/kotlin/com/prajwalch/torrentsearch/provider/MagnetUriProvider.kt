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
     * Resolves and returns the magnet URI from the given [url].
     */
    suspend fun getMagnetUri(url: String): String
}