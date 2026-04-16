package com.prajwalch.torrentsearch.domain

import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.data.repository.TorznabConfigRepository
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.TorznabConfig
import com.prajwalch.torrentsearch.providers.SearchProvider
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.providers.SearchProviderInfo
import com.prajwalch.torrentsearch.providers.SearchProviderSafetyStatus
import com.prajwalch.torrentsearch.providers.SearchProviderType
import com.prajwalch.torrentsearch.providers.TorznabSearchProvider

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

import javax.inject.Inject

data class SearchProviderInfoItem(
    val id: SearchProviderId,
    val name: String,
    val url: String,
    val specializedCategory: Category,
    val safetyStatus: SearchProviderSafetyStatus,
    val type: SearchProviderType,
    val isEnabled: Boolean,
)

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
            ?: return builtinProviders.filter { it.info.enabledByDefault }

        val enabledBuiltinProviders = builtinProviders.filter { it.info.id in enabledProviderIds }
        val enabledTorznabProviders = getEnabledTorznabProviders(enabledProviderIds)

        return enabledBuiltinProviders + enabledTorznabProviders
    }

    /**
     * Returns instances of enabled providers, filtering them by their
     * specialized category.
     */
    suspend fun getEnabledProvidersByCategory(category: Category): List<SearchProvider> {
        val enabledProviders = getEnabledProviders()
        if (category == Category.All) return enabledProviders

        return enabledProviders.filter {
            val specializedCategory = it.info.specializedCategory
            specializedCategory == Category.All || specializedCategory == category
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
     * Attempts to fina a [SearchProvider] associated with the given name.
     */
    fun findProviderByName(name: String): SearchProvider? {
        return builtinProviders.find { it.info.name == name }
    }

    /**
     * Returns [SearchProviderInfo]s of all search providers.
     */
    fun getProviderInfos(): Flow<List<SearchProviderInfoItem>> =
        combine(
            torznabConfigRepository.getAllConfigs(),
            settingsRepository.enabledSearchProvidersId,
        ) { torznabConfigs, enabledProvidersId ->
            val builtinProviderInfos = builtinProviders.map {
                it.info.toSearchProviderInfoItem(isEnabled = it.info.id in enabledProvidersId)
            }
            val torznabProviderInfos = torznabConfigs.map {
                it.toSearchProviderInfoItem(isEnabled = it.id in enabledProvidersId)
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
        val builtinProviderIds = builtinProviders.map { it.info.id }
        val torznabProviderIds = torznabConfigRepository.getAllConfigsId()
        val allIds = builtinProviderIds union torznabProviderIds

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
     * Disables NSFW and unsafe providers.
     */
    suspend fun disableRestrictedProviders() {
        val enabledProviderIds = settingsRepository.enabledSearchProvidersId.firstOrNull()
        if (enabledProviderIds.isNullOrEmpty()) return

        val enabledUnsafeProviderIds = builtinProviders
            .filter { it.info.id in enabledProviderIds }
            .filter { it.info.specializedCategory.isNSFW || it.info.safetyStatus.isUnsafe() }
            .map { it.info.id }
            .toSet()

        settingsRepository.setEnabledSearchProvidersId(enabledUnsafeProviderIds)
    }

    /**
     * Resets current providers setting to default.
     */
    suspend fun resetToDefault() {
        builtinProviders
            .filter { it.info.enabledByDefault }
            .map { it.info.id }
            .toSet()
            .let { settingsRepository.setEnabledSearchProvidersId(it) }
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

fun TorznabConfig.toSearchProviderInfoItem(isEnabled: Boolean) =
    SearchProviderInfoItem(
        id = this.id,
        name = this.searchProviderName,
        url = this.url,
        specializedCategory = this.category,
        safetyStatus = SearchProviderSafetyStatus.Safe,
        type = SearchProviderType.Torznab,
        isEnabled = isEnabled,
    )

fun SearchProviderInfo.toSearchProviderInfoItem(isEnabled: Boolean) =
    SearchProviderInfoItem(
        id = this.id,
        name = this.name,
        url = this.url,
        specializedCategory = this.specializedCategory,
        safetyStatus = this.safetyStatus,
        type = this.type,
        isEnabled = isEnabled,
    )