package com.prajwalch.torrentsearch.provider

import com.prajwalch.torrentsearch.domain.model.MagnetUriState

/**
 * A [SearchProvider] that can provide the magnet URI.
 *
 * This interface must be implemented for a provider which returns
 * torrents with [MagnetUriState.FetchRequired].
 */
interface MagnetUriProvider : SearchProvider {
    /**
     * Fetches the magnet URI from the [sourceUrl].
     */
    suspend fun getMagnetUri(sourceUrl: String): String
}