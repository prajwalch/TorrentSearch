package com.prajwalch.torrentsearch.domain

import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.data.repository.TorznabConfigRepository
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.CloudflareProtectionStatus
import com.prajwalch.torrentsearch.domain.model.SearchProviderInfo
import com.prajwalch.torrentsearch.domain.model.SearchProviderOrigin
import com.prajwalch.torrentsearch.domain.model.SearchProviderSafety
import com.prajwalch.torrentsearch.domain.model.TorznabConfig
import com.prajwalch.torrentsearch.domain.model.isUnsafe
import com.prajwalch.torrentsearch.network.NetworkClient
import com.prajwalch.torrentsearch.provider.LatestTorrentsProvider
import com.prajwalch.torrentsearch.provider.MagnetUriProvider
import com.prajwalch.torrentsearch.provider.SearchProvider
import com.prajwalch.torrentsearch.provider.SearchProviderId
import com.prajwalch.torrentsearch.provider.TopTorrentsProvider
import com.prajwalch.torrentsearch.provider.TorrentDetailsProvider
import com.prajwalch.torrentsearch.providers.TorznabSearchProvider

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

sealed interface ProtectionStatusUpdateResult {
    data object Error : ProtectionStatusUpdateResult

    data class Success(
        val numLockedProviders: Int,
        val numUnlockedProviders: Int,
    ) : ProtectionStatusUpdateResult
}

/**
 * A manager which is responsible for managing and handling all providers.
 */
class SearchProviderManager(
    private val builtinProviders: List<@JvmSuppressWildcards SearchProvider>,
    private val torznabConfigRepository: TorznabConfigRepository,
    private val settingsRepository: SettingsRepository,
    private val networkClient: NetworkClient,
) {
    /**
     * Returns a [Flow] of [SearchProviderInfo]s of all providers.
     */
    fun getProviderInfos(): Flow<List<SearchProviderInfo>> =
        combine(
            torznabConfigRepository.getAllConfigs(),
            settingsRepository.enabledProviderIds,
            settingsRepository.unlockedProviderIds,
        ) { torznabConfigs, enabledProviderIds, unlockedProviderIds ->
            val enabledProviderIds = enabledProviderIds.orEmpty()

            val builtinProviderInfos = builtinProviders
                .toSearchProviderInfos(enabledProviderIds, unlockedProviderIds)
            val torznabProviderInfos = torznabConfigs.toSearchProviderInfos(enabledProviderIds)

            builtinProviderInfos + torznabProviderInfos
        }

    /**
     * Returns a [Flow] of providers count.
     */
    fun getProvidersCount(): Flow<Int> = torznabConfigRepository.getConfigsCount()
        .map { torznabConfigCount -> builtinProviders.size + torznabConfigCount }

    /**
     * Returns a [Flow] of enabled providers count.
     */
    fun getEnabledProvidersCount(): Flow<Int> =
        getProviderInfos().map { infos -> infos.count { it.isEnabled } }

    /**
     * Returns instances of enabled providers, filtering them by their
     * specialized category.
     */
    suspend fun getEnabledProviders(category: Category = Category.All): List<SearchProvider> {
        val enabledProviders = getCurrentEnabledProviders()

        return if (category == Category.All) {
            enabledProviders
        } else {
            enabledProviders.filter { category in it.supportedCategories }
        }
    }

    /**
     * Returns a list of [LatestTorrentsProvider]s that are currently enabled.
     */
    suspend fun getEnabledLatestTorrentsProviders(
        category: Category = Category.All,
    ): List<LatestTorrentsProvider> {
        val enabledProviders = getCurrentEnabledProviders()
            .filterIsInstance<LatestTorrentsProvider>()

        return if (category == Category.All) {
            enabledProviders
        } else {
            enabledProviders.filter { category in it.supportedCategories }
        }
    }

    /**
     * Returns a list of [TopTorrentsProvider]s that are currently enabled.
     */
    suspend fun getEnabledTopTorrentsProviders(
        category: Category = Category.All,
    ): List<TopTorrentsProvider> {
        val enabledProviders = getCurrentEnabledProviders()
            .filterIsInstance<TopTorrentsProvider>()

        return if (category == Category.All) {
            enabledProviders
        } else {
            enabledProviders.filter { category in it.supportedCategories }
        }
    }

    /**
     * Finds a [SearchProvider] associated with the given [id].
     */
    suspend fun findEnabledProvider(id: SearchProviderId): SearchProvider? {
        return getCurrentEnabledProviders().find { it.id == id }
    }

    /**
     * Returns a list of providers that are currently enabled.
     */
    private suspend fun getCurrentEnabledProviders(): List<SearchProvider> {
        val enabledProviderIds = settingsRepository.currentEnabledProviderIds()
            ?: return emptyList()

        val enabledBuiltinProviders = builtinProviders.filter { it.id in enabledProviderIds }
        val enabledTorznabProviders = torznabConfigRepository
            .getCurrentConfigsByIds(enabledProviderIds)
            .map { config -> TorznabSearchProvider(config, networkClient) }

        return enabledBuiltinProviders + enabledTorznabProviders
    }

    /**
     * Finds a [MagnetUriProvider] associated with the given name.
     */
    fun findMagnetUriProviderByName(name: String): MagnetUriProvider? {
        return builtinProviders.filterIsInstance<MagnetUriProvider>()
            .find { it.name == name }
    }

    /**
     * Finds a [TorrentDetailsProvider] associated with the given name.
     */
    fun findDetailsProviderByName(name: String): TorrentDetailsProvider? {
        return builtinProviders.filterIsInstance<TorrentDetailsProvider>()
            .find { it.name == name }
    }

    /**
     * Finds a [TorrentDetailsProvider] associated with the given URL.
     */
    fun findDetailsProviderByUrl(url: String): TorrentDetailsProvider? {
        return builtinProviders.filterIsInstance<TorrentDetailsProvider>()
            .find { detailsProvider ->
                url.startsWith(detailsProvider.url) ||
                        detailsProvider.alternateUrlDomains.any { url.startsWith(it) }
            }
    }

    /**
     * Enables the provider associated with the given ID.
     */
    suspend fun enableProvider(id: SearchProviderId) {
        settingsRepository.addEnabledProviderId(id)
    }

//    /**
//     * Enables all providers.
//     */
//    suspend fun enableAllProviders() {
//        val builtinProviderIds = builtinProviders.map { it.id }.toSet()
//        val torznabProviderIds = torznabConfigRepository.getConfigIds()
//        val allIds = filterLockedProviderIds(builtinProviderIds) union torznabProviderIds
//
//        settingsRepository.setEnabledSearchProviderIds(allIds)
//    }

    /**
     * Enables providers associated with the given IDs.
     */
    suspend fun enableProvidersByIds(ids: Set<SearchProviderId>) {
        val filteredIds = filterLockedProviderIds(ids)
        settingsRepository.addEnabledProviderIds(filteredIds)
    }

    private suspend fun filterLockedProviderIds(
        ids: Set<SearchProviderId>,
    ): Set<SearchProviderId> {
        val unlockedProviderIds = settingsRepository.currentUnlockedProviderIds()

        // Torznab providers can't be locked, only built-ins can be.
        return builtinProviders
            .filter { it.id in ids }
            .filter { !it.isCloudflareProtected || it.id in unlockedProviderIds }
            .map { it.id }
            .toSet()
    }

    /**
     * Enables the default set of search providers.
     */
    suspend fun enableDefaultSearchProviders() {
        val defaultProviderIds = builtinProviders
            .filter { it.enabledByDefault && !it.isCloudflareProtected }
            .map { it.id }
            .toSet()

        settingsRepository.setEnabledProviderIds(defaultProviderIds)
    }

    /**
     * Skips enabling the default set of search providers.
     */
    suspend fun skipDefaultSearchProviders() {
        settingsRepository.setEnabledProviderIds(emptySet())
    }

    /**
     * Disables the provider associated with the given ID.
     */
    suspend fun disableProvider(id: SearchProviderId) {
        settingsRepository.removeEnabledProviderId(id)
    }

    /**
     * Disable providers associated with the given IDs.
     */
    suspend fun disableProviderByIds(ids: Set<SearchProviderId>) {
        settingsRepository.removeEnabledProviderIds(ids)
    }

    /**
     * Disables all unsafe providers which are currently enabled.
     */
    suspend fun disableUnsafeProviders() {
        val enabledProviders = getCurrentEnabledProviders()
        val nsfwCategories = Category.entries.filter { it.isNSFW }

        val unsafeProviderIds = enabledProviders
            .filter { enabledProvider ->
                nsfwCategories.any { it in enabledProvider.supportedCategories }
                        || enabledProvider.safety.isUnsafe()
            }
            .map { it.id }
            .toSet()

        settingsRepository.removeEnabledProviderIds(unsafeProviderIds)
    }

    /**
     * Unlocks the provider associated with the given ID.
     */
    suspend fun unlockProvider(id: SearchProviderId) {
        settingsRepository.addUnlockedProviderId(id)
    }

    /**
     * Locks and disables the provider associated with the given ID.
     */
    suspend fun lockProvider(id: SearchProviderId) {
        settingsRepository.removeUnlockedProviderId(id)
        builtinProviders.find { it.id == id }?.let {
            withContext(Dispatchers.IO) {
                NetworkClient.removeCookie(it.cloudflareSolverUrl ?: it.url)
            }
        }
        disableProvider(id)
    }

    /**
     * Updates the protection status of all protected providers.
     */
    suspend fun updateProtectionStatus(): ProtectionStatusUpdateResult =
        withContext(Dispatchers.IO) {
            val protectedProviders = builtinProviders.filter { it.isCloudflareProtected }
            var numLockedProviders = 0
            var numFailedProviders = 0

            for (provider in protectedProviders) {
                val providerId = provider.id
                val cloudflareSolverUrl = provider.cloudflareSolverUrl ?: provider.url

                val isUrlChallenged = try {
                    networkClient.isUrlChallenged(cloudflareSolverUrl)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    numFailedProviders += 1
                    continue
                }

                if (!isUrlChallenged) {
                    // Unlock it
                    settingsRepository.addUnlockedProviderId(providerId)
                    continue
                }

                // Lock
                settingsRepository.removeUnlockedProviderId(providerId)
                // Disable
                settingsRepository.removeEnabledProviderId(providerId)
                // Remove its cookie
                NetworkClient.removeCookie(cloudflareSolverUrl)

                numLockedProviders += 1
            }

            if (numFailedProviders == protectedProviders.size) {
                ProtectionStatusUpdateResult.Error
            } else {
                ProtectionStatusUpdateResult.Success(
                    numLockedProviders = numLockedProviders,
                    numUnlockedProviders = protectedProviders.size - numLockedProviders,
                )
            }
        }

    /**
     * Resets current providers settings to default.
     */
    suspend fun resetToDefault() {
        settingsRepository.setEnabledProviderIds(emptySet())
        settingsRepository.setUnlockedProviderIds(emptySet())
        NetworkClient.removeAllCookies()
    }

    /**
     * Creates and stores a new Torznab configuration.
     */
    suspend fun createTorznabConfig(
        searchProviderName: String,
        url: String,
        apiKey: String,
        supportedCategories: Set<Category>,
    ) {
        torznabConfigRepository.createConfig(
            searchProviderName = searchProviderName,
            url = url,
            apiKey = apiKey,
            supportedCategories = supportedCategories,
        )
    }

    /**
     * Finds the Torznab configuration associated with the given ID.
     */
    suspend fun findTorznabConfigById(id: String): TorznabConfig? {
        return torznabConfigRepository.findConfigById(id)
    }

    /**
     * Updates the configuration values of Torznab associated with the given
     * ID.
     */
    suspend fun updateTorznabConfig(
        id: String,
        searchProviderName: String,
        url: String,
        apiKey: String,
        supportedCategories: Set<Category>,
    ) {
        torznabConfigRepository.updateConfig(
            id = id,
            searchProviderName = searchProviderName,
            url = url,
            apiKey = apiKey,
            supportedCategories = supportedCategories,
        )
    }

    /**
     * Deletes the Torznab configuration associated with the given ID.
     */
    suspend fun deleteTorznabConfig(id: String) {
        torznabConfigRepository.deleteConfigById(id)
        settingsRepository.removeEnabledProviderId(id)
    }
}

private fun List<SearchProvider>.toSearchProviderInfos(
    enabledProviderIds: Set<SearchProviderId>,
    unlockedProviderIds: Set<SearchProviderId>,
): List<SearchProviderInfo> = map {
    val isEnabled = it.id in enabledProviderIds
    val protectionStatus = when {
        !it.isCloudflareProtected -> CloudflareProtectionStatus.UnProtected
        it.id in unlockedProviderIds -> CloudflareProtectionStatus.Unlocked
        else -> CloudflareProtectionStatus.Locked
    }

    it.toSearchProviderInfo(
        isEnabled = isEnabled,
        protectionStatus = protectionStatus,
    )
}

private fun SearchProvider.toSearchProviderInfo(
    isEnabled: Boolean,
    protectionStatus: CloudflareProtectionStatus,
) = SearchProviderInfo(
    id = this.id,
    name = this.name,
    url = this.url,
    cloudflareSolverUrl = this.cloudflareSolverUrl,
    supportedCategories = this.supportedCategories,
    safety = this.safety,
    origin = SearchProviderOrigin.Builtin,
    cloudflareProtectionStatus = protectionStatus,
    isEnabled = isEnabled,
)

private fun List<TorznabConfig>.toSearchProviderInfos(
    enabledProviderIds: Set<SearchProviderId>,
): List<SearchProviderInfo> = map {
    it.toSearchProviderInfo(it.id in enabledProviderIds)
}

private fun TorznabConfig.toSearchProviderInfo(isEnabled: Boolean) =
    SearchProviderInfo(
        id = this.id,
        name = this.searchProviderName,
        url = this.url,
        supportedCategories = this.supportedCategories,
        safety = SearchProviderSafety.Safe,
        origin = SearchProviderOrigin.Torznab,
        cloudflareProtectionStatus = CloudflareProtectionStatus.UnProtected,
        isEnabled = isEnabled,
    )