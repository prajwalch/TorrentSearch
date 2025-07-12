package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.data.SearchProvider

typealias ProviderId = String

object SearchProviders {
    private val allProviders = mapOf(
        Pair("1", Eztv()),
        Pair("2", NyaaSi()),
        Pair("3", ThePirateBay()),
        Pair("4", TheRarBg()),
        Pair("5", TorrentsCsv()),
        Pair("6", Yts()),
        Pair("7", AnimeTosho()),
        Pair("8", LimeTorrents()),
        Pair("9", MyPornClub()),
    )

    fun get(ids: Set<ProviderId>): List<SearchProvider> {
        return allProviders.mapNotNull { (id, provider) ->
            if (ids.contains(id)) provider else null
        }
    }

    fun namesWithId(): List<Pair<ProviderId, String>> {
        return allProviders.map { (id, provider) ->
            Pair(id, provider::class.simpleName!!)
        }
    }

    fun ids(): Set<ProviderId> {
        return allProviders.map { (id, _) -> id }.toSet()
    }
}