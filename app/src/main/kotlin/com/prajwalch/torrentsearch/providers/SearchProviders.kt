package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.data.SearchProvider
import com.prajwalch.torrentsearch.data.SearchProviderId

object SearchProviders {
    private val allProviders = setOf(
        Eztv(id = "1"),
        NyaaSi(id = "2"),
        ThePirateBay(id = "3"),
        TheRarBg(id = "4"),
        TorrentsCsv(id = "5"),
        Yts(id = "6"),
        AnimeTosho(id = "7"),
        LimeTorrents(id = "8"),
        MyPornClub(id = "9"),
        Knaben(id = "10"),
    )

    fun get(ids: Set<SearchProviderId>): List<SearchProvider> {
        return allProviders.filter { ids.contains(it.id) }
    }

    fun namesWithId(): List<Pair<SearchProviderId, String>> {
        return allProviders.map { Pair(it.id, it.name) }
    }

    fun ids(): Set<SearchProviderId> {
        return allProviders.map { it.id }.toSet()
    }
}