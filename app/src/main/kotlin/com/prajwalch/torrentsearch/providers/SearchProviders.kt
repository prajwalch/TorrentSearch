package com.prajwalch.torrentsearch.providers

import com.prajwalch.torrentsearch.data.SearchProvider
import com.prajwalch.torrentsearch.data.SearchProviderId

object SearchProviders {
    private val enabledByDefault = setOf(
        Eztv(id = "p1"),
        Knaben(id = "p2"),
        NyaaSi(id = "p3"),
        TorrentsCsv(id = "p4"),
        Yts(id = "p5"),
    )

    private val disabledByDefault = setOf(
        AnimeTosho(id = "p6"),
        LimeTorrents(id = "p7"),
        MyPornClub(id = "p8"),
        ThePirateBay(id = "p9"),
        TheRarBg(id = "p10"),
    )

    private val all = enabledByDefault.plus(disabledByDefault).sortedBy { it.name }

    fun get(ids: Set<SearchProviderId>): List<SearchProvider> {
        return all.filter { ids.contains(it.id) }
    }

    fun enabledIds(): Set<SearchProviderId> {
        return enabledByDefault.map { it.id }.toSet()
    }

    fun namesWithId(): List<Pair<SearchProviderId, String>> {
        return all.map { Pair(it.id, it.name) }
    }
}