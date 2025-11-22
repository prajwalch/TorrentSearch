package com.prajwalch.torrentsearch.data.repository

import com.prajwalch.torrentsearch.data.local.dao.TorznabSearchProviderDao
import com.prajwalch.torrentsearch.data.local.entities.toEntity
import com.prajwalch.torrentsearch.data.local.entities.toSearchProviderInfo
import com.prajwalch.torrentsearch.data.local.entities.toTorznabConfig
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.providers.AnimeTosho
import com.prajwalch.torrentsearch.providers.BitSearch
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

import javax.inject.Inject

class SearchProvidersRepository @Inject constructor(
    private val dao: TorznabSearchProviderDao,
) {
    private val builtins = listOf(
        AnimeTosho(),
        BitSearch(),
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

    // TODO: Remove this or handle enabled by default search providers properly.
    fun getEnabledSearchProvidersId(): Set<SearchProviderId> {
        return builtins.filter { it.info.enabledByDefault }.map { it.info.id }.toSet()
    }

    fun observeSearchProvidersInfo(): Flow<List<SearchProviderInfo>> {
        val builtinSearchProvidersInfoFlow = flowOf(builtins.map { it.info })
        val torznabSearchProvidersInfoFlow = dao.observeAll().map { it.toSearchProviderInfo() }

        return combine(
            builtinSearchProvidersInfoFlow,
            torznabSearchProvidersInfoFlow
        ) { builtinInfos, torznabInfos ->
            builtinInfos + torznabInfos
        }
    }

    fun observeSearchProvidersCount(): Flow<Int> {
        return dao.observeCount().map { it + builtins.size }
    }

    suspend fun getSearchProvidersInstance(category: Category): List<SearchProvider> {
        val searchProviders = getSearchProvidersInstance()

        if (category == Category.All) {
            return searchProviders
        }

        return searchProviders.filter {
            // NOTE: Currently, if the search provider's specialized is set to
            //       `All` we can't surely know whether the underlying server
            //       supports setting specific category or not.
            //
            //       For example: TorrentCSV does allow to search any type of
            //       torrent but it doesn't support setting specific category
            //       explicitly. Meaning, we can't ask it to search torrents
            //       of specific category.
            //
            //       To address this issue, force each and every search provider
            //       to list out all categories they allow or support to set
            //       explicitly instead of single `specializedCategory`.
            (it.info.specializedCategory == Category.All) || (category == it.info.specializedCategory)
        }
    }

    suspend fun getSearchProvidersInstance(): List<SearchProvider> {
        val builtinSearchProvidersFlow = flowOf(builtins)
        val torznabSearchProvidersFlow = dao.observeAll().map { entities ->
            entities.map { TorznabSearchProvider(config = it.toTorznabConfig()) }
        }

        return combine(
            builtinSearchProvidersFlow,
            torznabSearchProvidersFlow,
        ) { builtins, externals ->
            builtins + externals
        }.firstOrNull().orEmpty()
    }

    suspend fun addTorznabSearchProvider(config: TorznabSearchProviderConfig) {
        val config = config.copy(url = config.url.trimEnd { it == '/' })
        dao.insert(searchProvider = config.toEntity())
    }

    suspend fun findTorznabSearchProviderConfig(
        id: SearchProviderId,
    ): TorznabSearchProviderConfig? {
        return dao.findById(id = id)?.toTorznabConfig()
    }

    suspend fun updateTorznabSearchProvider(config: TorznabSearchProviderConfig) {
        dao.update(searchProvider = config.toEntity())
    }

    suspend fun deleteTorznabSearchProvider(id: String) {
        dao.deleteById(id = id)
    }
}