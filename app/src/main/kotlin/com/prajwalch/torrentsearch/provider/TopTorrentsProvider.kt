package com.prajwalch.torrentsearch.provider

import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.Torrent

/**
 * A [SearchProvider] that can provide the top/trending torrents.
 */
interface TopTorrentsProvider : SearchProvider {
    /**
     * Fetches top/trending torrents of the given [category].
     */
    suspend fun getTopTorrents(category: Category = Category.All): List<Torrent>
}