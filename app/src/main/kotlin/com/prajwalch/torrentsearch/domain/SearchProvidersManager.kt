package com.prajwalch.torrentsearch.domain

import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.data.repository.TorznabConfigRepository
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.CloudflareProtectionStatus
import com.prajwalch.torrentsearch.domain.model.SearchProviderInfo
import com.prajwalch.torrentsearch.domain.model.TorznabConfig
import com.prajwalch.torrentsearch.network.HttpClient
import com.prajwalch.torrentsearch.providers.LatestTorrentsProvider
import com.prajwalch.torrentsearch.providers.SearchProvider
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.providers.SearchProviderSafetyStatus
import com.prajwalch.torrentsearch.providers.SearchProviderType
import com.prajwalch.torrentsearch.providers.TopTorrentsProvider
import com.prajwalch.torrentsearch.providers.TorrentDetailsProvider
import com.prajwalch.torrentsearch.providers.TorznabSearchProvider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

import javax.inject.Inject

/**
 * A search providers manager which is responsible for managing and handling
 * all providers specific task and responsibility.
 */
class SearchProvidersManager @Inject constructor(
    private val builtinProviders: List<@JvmSuppressWildcards SearchProvider>,
    private val torznabConfigRepository: TorznabConfigRepository,
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Returns instances of enabled providers.
     */
    suspend fun getEnabledProviders(): List<SearchProvider> {
        val enabledProviderIds = settingsRepository.enabledSearchProvidersId.firstOrNull()
            ?: return builtinProviders.filter { it.enabledByDefault }

        val enabledBuiltinProviders = builtinProviders.filter { it.id in enabledProviderIds }
        val enabledTorznabProviders = getEnabledTorznabProviders(enabledProviderIds)

        return enabledBuiltinProviders + enabledTorznabProviders
    }

    /**
     * Returns instances of enabled providers, filtering them by their
     * specialized category.
     */
    suspend fun getEnabledProvidersByCategory(category: Category): List<SearchProvider> {
        val enabledProviders = getEnabledProviders()

        return if (category == Category.All) {
            enabledProviders
        } else {
            enabledProviders.filter { category in it.supportedCategories }
        }
    }

    /**
     * Returns instances of enabled Torznab providers.
     */
    private suspend fun getEnabledTorznabProviders(
        enabledProviderIds: Set<SearchProviderId>,
    ): List<TorznabSearchProvider> = torznabConfigRepository.getAllConfigs()
        .map { configs -> configs.filter { it.id in enabledProviderIds } }
        .map { configs -> configs.map(::TorznabSearchProvider) }
        .firstOrNull()
        .orEmpty()

    /**
     * Finds a torrent details provider associated with the given name.
     */
    fun findDetailsProviderByName(name: String): TorrentDetailsProvider? {
        return builtinProviders
            .filterIsInstance<TorrentDetailsProvider>()
            .find { it.name == name }
    }

    /**
     * Finds a torrent details provider associated with the given URL.
     */
    fun findDetailsProviderByUrl(url: String): TorrentDetailsProvider? {
        return builtinProviders.filterIsInstance<TorrentDetailsProvider>()
            .find { detailsProvider ->
                if (url.startsWith(detailsProvider.url)) {
                    true
                } else {
                    detailsProvider.alternateUrlDomains.any { url.startsWith(it) }
                }
            }
    }

    /**
     * Returns a list containing instances of latest torrents providers that
     * are currently enabled.
     */
    suspend fun getEnabledLatestTorrentsProviders(category: Category): List<LatestTorrentsProvider> {
        val enabledProviderIds = settingsRepository.enabledSearchProvidersId.firstOrNull().orEmpty()
        val enabledProviders = builtinProviders.filterIsInstance<LatestTorrentsProvider>()
            .filter { it.id in enabledProviderIds }

        return if (category == Category.All) {
            enabledProviders
        } else {
            enabledProviders.filter { category in it.supportedCategories }
        }
    }

    /**
     * Returns a list containing instances of top torrents providers that are
     * currently enabled.
     */
    suspend fun getEnabledTopTorrentsProviders(category: Category): List<TopTorrentsProvider> {
        val enabledProviderIds = settingsRepository.enabledSearchProvidersId.firstOrNull().orEmpty()
        val enabledProviders = builtinProviders.filterIsInstance<TopTorrentsProvider>()
            .filter { it.id in enabledProviderIds }

        return if (category == Category.All) {
            enabledProviders
        } else {
            enabledProviders.filter { category in it.supportedCategories }
        }
    }

    /**
     * Returns [SearchProviderInfo]s of all search providers.
     */
    fun getProviderInfos(): Flow<List<SearchProviderInfo>> =
        combine(
            torznabConfigRepository.getAllConfigs(),
            settingsRepository.enabledSearchProvidersId,
            settingsRepository.protectionUnlockedProviderIds,
        ) { torznabConfigs, enabledProvidersId, protectionUnlockedProviderIds ->
            val builtinProviderInfos = builtinProviders.map {
                val cloudflareProtectionStatus = when {
                    !it.isCloudflareProtected -> CloudflareProtectionStatus.UnProtected
                    it.id in protectionUnlockedProviderIds -> CloudflareProtectionStatus.Unlocked
                    else -> CloudflareProtectionStatus.Locked
                }
                it.getInfo(isEnabled = it.id in enabledProvidersId, cloudflareProtectionStatus)
            }
            val torznabProviderInfos = torznabConfigs.map {
                it.getInfo(isEnabled = it.id in enabledProvidersId)
            }

            builtinProviderInfos + torznabProviderInfos
        }

    /**
     * Returns providers count as [Flow].
     */
    fun getProvidersCount(): Flow<Int> = torznabConfigRepository.getConfigsCount()
        .map { torznabConfigCount -> builtinProviders.size + torznabConfigCount }

    /**
     * Enables the provider associated with the given ID.
     */
    suspend fun enableProvider(id: SearchProviderId) {
        settingsRepository.addEnabledSearchProviderId(id)
    }

    /**
     * Enables all providers.
     */
    suspend fun enableAllProviders() {
        val builtinProviderIds = builtinProviders.map { it.id }
        val torznabProviderIds = torznabConfigRepository.getAllConfigsId()
        val allIds = builtinProviderIds union torznabProviderIds

        settingsRepository.setEnabledSearchProvidersId(allIds)
    }

    /**
     * Enables providers associated with the given IDs.
     */
    suspend fun enableProviderByIds(ids: Set<SearchProviderId>) {
        val currentEnabledProviderIds = settingsRepository.enabledSearchProvidersId
            .firstOrNull()
            .orEmpty()
        val allIds = currentEnabledProviderIds + ids

        settingsRepository.setEnabledSearchProvidersId(allIds)
    }

    /**
     * Disables the provider associated with the given ID.
     */
    suspend fun disableProvider(id: SearchProviderId) {
        settingsRepository.removeEnabledSearchProviderId(id)
    }

    /**
     * Disables all providers.
     */
    suspend fun disableAllProviders() {
        settingsRepository.setEnabledSearchProvidersId(emptySet())
    }

    /**
     * Disable providers associated with the given IDs.
     */
    suspend fun disableProviderByIds(ids: Set<SearchProviderId>) {
        val currentEnabledProviderIds = settingsRepository.enabledSearchProvidersId
            .firstOrNull()
            .orEmpty()
        val allIds = currentEnabledProviderIds - ids

        settingsRepository.setEnabledSearchProvidersId(allIds)
    }

    /**
     * Disables NSFW and unsafe providers.
     */
    suspend fun disableRestrictedProviders() {
        val enabledProviderIds = settingsRepository.enabledSearchProvidersId.firstOrNull()
        if (enabledProviderIds.isNullOrEmpty()) return

        val nsfwCategories = Category.entries.filter { it.isNSFW }
        val enabledUnsafeProviderIds = builtinProviders
            .filter { it.id in enabledProviderIds }
            .filter {
                it.supportedCategories.containsAll(nsfwCategories) || it.safetyStatus.isUnsafe()
            }
            .map { it.id }
            .toSet()

        settingsRepository.setEnabledSearchProvidersId(enabledUnsafeProviderIds)
    }

    /**
     * Unlocks the provider associated with the given ID.
     */
    suspend fun unlockProvider(id: SearchProviderId) {
        settingsRepository.addProtectionUnlockedProviderId(id)
    }

    /**
     * Locks and disables the provider associated with the given ID.
     */
    suspend fun lockProvider(id: SearchProviderId) {
        settingsRepository.removeProtectionUnlockedProviderId(id)
        builtinProviders.find { it.id == id }?.let {
            withContext(Dispatchers.IO) {
                HttpClient.removeCookie(it.cloudflareSolverUrl ?: it.url)
            }
        }
        disableProvider(id)
    }

    /**
     * Resets current providers setting to default.
     */
    suspend fun resetToDefault() {
        builtinProviders
            .filter { it.enabledByDefault }
            .map { it.id }
            .toSet()
            .let { settingsRepository.setEnabledSearchProvidersId(it) }
        settingsRepository.setProtectionUnlockedProviderIds(emptySet())
        HttpClient.removeAllCookies()
    }

    /**
     * Creates and stores a new Torznab config using the given values.
     */
    suspend fun createTorznabConfig(
        searchProviderName: String,
        url: String,
        apiKey: String,
        category: Category,
    ) {
        torznabConfigRepository.createConfig(
            searchProviderName = searchProviderName,
            url = url,
            apiKey = apiKey,
            category = category,
        )
    }

    /**
     * Attempts to find the existing Torznab config associated with the given ID.
     */
    suspend fun findTorznabConfigById(id: String): TorznabConfig? {
        return torznabConfigRepository.findConfigById(id)
    }

    /**
     * Updates the Torznab config associated with the given ID with the
     * given new values.
     */
    suspend fun updateTorznabConfig(
        id: String,
        searchProviderName: String,
        url: String,
        apiKey: String,
        category: Category,
    ) {
        torznabConfigRepository.updateConfig(
            id = id,
            searchProviderName = searchProviderName,
            url = url,
            apiKey = apiKey,
            category = category,
        )
    }

    /**
     * Deletes the existing Torznab config associated with the given ID.
     */
    suspend fun deleteTorznabConfig(id: String) {
        torznabConfigRepository.deleteConfigById(id)
        settingsRepository.removeEnabledSearchProviderId(id)
    }
}

private fun SearchProvider.getInfo(
    isEnabled: Boolean,
    protectionStatus: CloudflareProtectionStatus,
) = SearchProviderInfo(
    id = this.id,
    name = this.name,
    url = this.url,
    cloudflareSolverUrl = this.cloudflareSolverUrl,
    supportedCategories = this.supportedCategories,
    safetyStatus = this.safetyStatus,
    type = this.type,
    cloudflareProtectionStatus = protectionStatus,
    isEnabled = isEnabled,
)

private fun TorznabConfig.getInfo(isEnabled: Boolean) =
    SearchProviderInfo(
        id = this.id,
        name = this.searchProviderName,
        url = this.url,
        supportedCategories = setOf(this.category),
        safetyStatus = SearchProviderSafetyStatus.Safe,
        type = SearchProviderType.Torznab,
        cloudflareProtectionStatus = CloudflareProtectionStatus.UnProtected,
        isEnabled = isEnabled,
    )