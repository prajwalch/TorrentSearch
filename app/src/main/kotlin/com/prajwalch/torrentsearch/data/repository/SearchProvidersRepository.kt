package com.prajwalch.torrentsearch.data.repository

import com.prajwalch.torrentsearch.data.database.dao.TorznabSearchProviderDao
import com.prajwalch.torrentsearch.data.database.entities.TorznabSearchProviderEntity
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.providers.AnimeTosho
import com.prajwalch.torrentsearch.providers.Eztv
import com.prajwalch.torrentsearch.providers.Knaben
import com.prajwalch.torrentsearch.providers.LimeTorrents
import com.prajwalch.torrentsearch.providers.MyPornClub
import com.prajwalch.torrentsearch.providers.Nyaa
import com.prajwalch.torrentsearch.providers.SearchProvider
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.providers.SearchProviderInfo
import com.prajwalch.torrentsearch.providers.SearchProviderSafetyStatus
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

import javax.inject.Inject

class SearchProvidersRepository @Inject constructor(private val dao: TorznabSearchProviderDao) {
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
    suspend fun addTorznabSearchProvider(config: TorznabSearchProviderConfig) {
        val url = config.url.trimEnd { it == '/' }
        val unsafeReason = when (config.safetyStatus) {
            is SearchProviderSafetyStatus.Safe -> null
            is SearchProviderSafetyStatus.Unsafe -> config.safetyStatus.reason
        }

        val dbEntity = TorznabSearchProviderEntity(
            name = config.name,
            url = url,
            apiKey = config.apiKey,
            category = config.category.name,
            unsafeReason = unsafeReason,
        )
        dao.insert(searchProvider = dbEntity)
    }

    /**
     * Returns the config of Torznab search provider that matches the specified
     * ID, if exists.
     */
    suspend fun findTorznabSearchProviderConfig(
        id: SearchProviderId,
    ): TorznabSearchProviderConfig? = dao.findById(id = id)?.toConfig()

    /**
     * Updates the Torznab search provider that matches the specified ID
     * with the given configuration.
     */
    suspend fun updateTorznabSearchProvider(config: TorznabSearchProviderConfig) {
        val unsafeReason = when (config.safetyStatus) {
            is SearchProviderSafetyStatus.Safe -> null
            is SearchProviderSafetyStatus.Unsafe -> config.safetyStatus.reason
        }
        val dbEntity = TorznabSearchProviderEntity(
            id = config.id,
            name = config.name,
            url = config.url,
            apiKey = config.apiKey,
            category = config.category.name,
            unsafeReason = unsafeReason,
        )
        dao.update(searchProvider = dbEntity)
    }

    /** Deletes the Torznab search provider that matches the specified ID. */
    suspend fun deleteTorznabSearchProvider(id: String) {
        dao.deleteById(id = id)
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

private fun TorznabSearchProviderEntity.toConfig(): TorznabSearchProviderConfig {
    val safetyStatus = this.unsafeReason
        ?.let { SearchProviderSafetyStatus.Unsafe(reason = it) }
        ?: SearchProviderSafetyStatus.Safe

    return TorznabSearchProviderConfig(
        id = this.id,
        name = this.name,
        url = this.url,
        apiKey = this.apiKey,
        category = Category.valueOf(this.category),
        safetyStatus = safetyStatus
    )
}