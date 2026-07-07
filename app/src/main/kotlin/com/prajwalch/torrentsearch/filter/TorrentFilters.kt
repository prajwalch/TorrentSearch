package com.prajwalch.torrentsearch.filter

import com.prajwalch.torrentsearch.domain.model.Category

object TorrentFilters {
    fun isSfw(): TorrentFilter = { torrent -> !torrent.isNSFW }

    fun isAlive(): TorrentFilter = { torrent -> !torrent.isDead }

    fun notExcludedProvider(excludedProviders: Set<String>): TorrentFilter =
        { torrent -> torrent.providerName !in excludedProviders }

    fun notViewed(viewedTorrentHashes: Set<String>): TorrentFilter =
        { torrent -> torrent.infoHash !in viewedTorrentHashes }

    fun matchesCategory(category: Category): TorrentFilter =
        { torrent -> category == torrent.category }

    fun matchesQuery(query: String): TorrentFilter {
        val words = query.split(' ', ignoreCase = true).filter { it.isNotBlank() }

        return { torrent ->
            words.any { word -> torrent.name.contains(word, ignoreCase = true) }
        }
    }
}