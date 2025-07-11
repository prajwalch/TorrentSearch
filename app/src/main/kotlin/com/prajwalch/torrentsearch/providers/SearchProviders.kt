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
    )

    private var enabledProviders = ids()

    fun namesWithId(): List<Pair<ProviderId, String>> {
        return allProviders.map { (id, provider) ->
            Pair(id, provider::class.simpleName!!)
        }
    }

    fun ids(): Set<ProviderId> {
        return allProviders.map { (id, _) -> id }.toSet()
    }

    fun enabled(): List<SearchProvider> {
        return allProviders
            .filter { (id, _) -> enabledProviders.contains(id) }
            .map { (_, provider) -> provider }
    }

    fun setEnabledProviders(providers: Set<ProviderId>) {
        enabledProviders = providers
    }
}