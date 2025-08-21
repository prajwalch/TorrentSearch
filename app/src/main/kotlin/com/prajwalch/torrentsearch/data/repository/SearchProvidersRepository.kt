package com.prajwalch.torrentsearch.data.repository

import com.prajwalch.torrentsearch.data.database.dao.TorznabSearchProviderDao
import com.prajwalch.torrentsearch.data.database.entities.TorznabSearchProviderEntity
import com.prajwalch.torrentsearch.providers.AnimeTosho
import com.prajwalch.torrentsearch.providers.Eztv
import com.prajwalch.torrentsearch.providers.Knaben
import com.prajwalch.torrentsearch.providers.LimeTorrents
import com.prajwalch.torrentsearch.providers.MyPornClub
import com.prajwalch.torrentsearch.providers.Nyaa
import com.prajwalch.torrentsearch.providers.SearchProvider
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.providers.SearchProviderInfo
import com.prajwalch.torrentsearch.providers.Sukebei
import com.prajwalch.torrentsearch.providers.ThePirateBay
import com.prajwalch.torrentsearch.providers.TheRarBg
import com.prajwalch.torrentsearch.providers.TokyoToshokan
import com.prajwalch.torrentsearch.providers.TorrentDownloads
import com.prajwalch.torrentsearch.providers.TorrentsCSV
import com.prajwalch.torrentsearch.providers.TorznabSearchProvider
import com.prajwalch.torrentsearch.providers.TorznabSearchProviderConfig
import com.prajwalch.torrentsearch.providers.UIndex
import com.prajwalch.torrentsearch.providers.XXXClub
import com.prajwalch.torrentsearch.providers.Yts

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SearchProvidersRepository(private val dao: TorznabSearchProviderDao) {
    /**
     * Instances of built-in search providers.
     *
     * Make sure they're sorted.
     */
    private val builtins = setOf(
        AnimeTosho(),
        Eztv(),
        Knaben(),
        LimeTorrents(),
        MyPornClub(),
        Nyaa(),
        Sukebei(),
        ThePirateBay(),
        TheRarBg(),
        TokyoToshokan(),
        TorrentDownloads(),
        TorrentsCSV(),
        UIndex(),
        XXXClub(),
        Yts(),
    )

    /** Adds a new Torznab API compatible search provider. */
    suspend fun addTorznabSearchProvider(
        name: String,
        url: String,
        apiKey: String,
    ) {
        // FIXME: Not a reliable way to generate an ID.
        val id = "torznab$name"
        val url = url.trimEnd { it == '/' }

        val dbEntity = TorznabSearchProviderEntity(
            id = id,
            name = name,
            url = url,
            apiKey = apiKey,
        )
        dao.insert(searchProvider = dbEntity)
    }

    /** Returns a list containing the info of all search providers. */
    fun searchProvidersInfo(): Flow<List<SearchProviderInfo>> =
        getInstances().map { searchProviders -> searchProviders.map { it.info } }

    /** Returns the count of search providers. */
    fun count(): Flow<Int> = dao.getCount().map { it + builtins.size }

    /**
     * Returns a set containing the ID of search providers that are enabled
     * by default.
     */
    fun defaultEnabledIds(): Set<SearchProviderId> =
        builtins.filter { it.info.enabledByDefault }.map { it.info.id }.toSet()

    fun getInstances(): Flow<List<SearchProvider>> = dao.getAll().map { entities ->
        entities
            .map { TorznabSearchProvider(config = it.toConfig()) }
            .plus(builtins)
            .sortedBy { it.info.name }
    }
}

private fun TorznabSearchProviderEntity.toConfig() =
    TorznabSearchProviderConfig(
        id = this.id,
        name = this.name,
        url = this.url,
        apiKey = this.apiKey,
    )