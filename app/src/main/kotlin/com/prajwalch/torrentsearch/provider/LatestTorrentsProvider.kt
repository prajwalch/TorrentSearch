package com.prajwalch.torrentsearch.provider

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent

/**
 * A [SearchProvider] that can provide latest/recent torrents.
 */
interface LatestTorrentsProvider : SearchProvider {
    /**
     * Fetches latest torrents of the given [category].
     */
    suspend fun getLastestTorrents(category: Category = Category.All): List<Torrent>
}