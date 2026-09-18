package com.prajwalch.torrentsearch.ui.searchproviders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.domain.SearchProviderManager
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.CloudflareProtectionStatus
import com.prajwalch.torrentsearch.domain.model.SearchProviderInfo
import com.prajwalch.torrentsearch.provider.SearchProviderId

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

data class SearchProvidersUiState(
    val searchProviders: List<SearchProviderInfo> = emptyList(),
    val filter: SearchProviderFilter = SearchProviderFilter(),
    val totalNumProviders: Int = 0,
    val enabledProvidersCount: Int = 0,
    val protectionUpdateState: ProtectionUpdateState = ProtectionUpdateState.Idle,
)

data class SearchProviderFilter(
    val category: Category = Category.All,
    val protection: SearchProviderProtection? = null,
)

enum class SearchProviderProtection {
    Protected,
    Locked,
    Unlocked,
}

sealed interface ProtectionUpdateState {
    data object Idle : ProtectionUpdateState
    data object Updating : ProtectionUpdateState
    data class Complete(
        val numLockedProviders: Int,
        val numUnlockedProviders: Int,
    ) : ProtectionUpdateState
}

/** ViewModel which handles the business logic of Search providers screen. */
@KoinViewModel
class SearchProvidersViewModel(
    private val searchProviderManager: SearchProviderManager,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val providerInfosProcessor =
        SearchProviderInfosProcessor(searchProviderManager.getProviderInfos())

    private val protectionUpdateState =
        MutableStateFlow<ProtectionUpdateState>(ProtectionUpdateState.Idle)

    val uiState: StateFlow<SearchProvidersUiState> =
        combine(
            providerInfosProcessor.result,
            protectionUpdateState,
            searchProviderManager.getProvidersCount(),
            settingsRepository.enabledSearchProviderIds.map { it?.size ?: 0 },
        ) {
                processorResult,
                protectionUpdateState,
                totalNumProviders,
                enabledProvidersCount,
            ->
            SearchProvidersUiState(
                filter = processorResult.filter,
                searchProviders = processorResult.infos,
                totalNumProviders = totalNumProviders,
                enabledProvidersCount = enabledProvidersCount,
                protectionUpdateState = protectionUpdateState,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = SearchProvidersUiState(),
        )

    /** Enables/disables search provider matching the specified ID. */
    fun enableSearchProvider(providerId: SearchProviderId, enable: Boolean) {
        viewModelScope.launch {
            if (enable) {
                searchProviderManager.enableProvider(providerId)
            } else {
                searchProviderManager.disableProvider(providerId)
            }
        }
    }

    /** Enables all search providers. */
    fun enableAllSearchProviders() {
        viewModelScope.launch {
            // Respect filter.
            val providerIds = uiState.value.searchProviders.map { it.id }.toSet()
            searchProviderManager.enableProvidersByIds(providerIds)
        }
    }

    /** Disables all search providers. */
    fun disableAllSearchProviders() {
        viewModelScope.launch {
            // Respect filter.
            val providerIds = uiState.value.searchProviders.map { it.id }.toSet()
            searchProviderManager.disableProviderByIds(providerIds)
        }
    }

    fun updateProtectionStatus() {
        protectionUpdateState.value = ProtectionUpdateState.Updating

        viewModelScope.launch {
            val result = searchProviderManager.updateProtectionStatus()
            protectionUpdateState.value = ProtectionUpdateState.Complete(
                numLockedProviders = result.numLockedProviders,
                numUnlockedProviders = result.numUnlockedProviders,
            )
        }
    }

    fun resetProtectionUpdateState() {
        protectionUpdateState.value = ProtectionUpdateState.Idle
    }

    /** Resets enabled search providers to default. */
    fun resetEnabledSearchProvidersToDefault() {
        viewModelScope.launch {
            searchProviderManager.resetToDefault()
        }
    }

    /** Deletes the Torznab search provider that matches the specified ID. */
    fun deleteTorznabConfig(id: String) {
        viewModelScope.launch {
            searchProviderManager.deleteTorznabConfig(id)
        }
    }

    fun filterSearchProviders(query: String) {
        providerInfosProcessor.filterByQuery(query)
    }

    /** Selects/unselects the given category. */
    fun toggleCategory(category: Category) {
        providerInfosProcessor.toggleCategory(category)
    }

    fun toggleProviderProtection(protection: SearchProviderProtection) {
        providerInfosProcessor.toggleProviderProtection(protection)
    }

    fun markProviderAsUnlocked(id: SearchProviderId) {
        viewModelScope.launch {
            searchProviderManager.unlockProvider(id)
        }
    }
}

private class SearchProviderInfosProcessor(
    searchProviderInfos: Flow<List<SearchProviderInfo>>,
) {
    data class ProcessResult(
        val infos: List<SearchProviderInfo>,
        val filter: SearchProviderFilter,
    )

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(SearchProviderFilter())

    val result: Flow<ProcessResult> =
        combine(
            searchProviderInfos,
            query,
            filter,
            ::processProviderInfos,
        ).flowOn(Dispatchers.Default)

    private fun processProviderInfos(
        infos: List<SearchProviderInfo>,
        query: String,
        filter: SearchProviderFilter,
    ): ProcessResult {
        val predicates = buildFilterPredicates(query, filter)
        val filteredInfos = infos.filter { info ->
            predicates.all { predicate -> predicate(info) }
        }

        return ProcessResult(filteredInfos, filter)
    }

    private fun buildFilterPredicates(
        query: String,
        filter: SearchProviderFilter,
    ): List<(SearchProviderInfo) -> Boolean> = buildList {
        when (filter.protection) {
            SearchProviderProtection.Protected -> add {
                it.cloudflareProtectionStatus != CloudflareProtectionStatus.UnProtected
            }

            SearchProviderProtection.Locked -> add {
                it.cloudflareProtectionStatus == CloudflareProtectionStatus.Locked
            }

            SearchProviderProtection.Unlocked -> add {
                it.cloudflareProtectionStatus == CloudflareProtectionStatus.Unlocked
            }

            else -> {}
        }

        if (filter.category != Category.All) {
            add { it.supportedCategories.contains(filter.category) }
        }

        if (query.isNotBlank()) {
            add { it.name.contains(query, ignoreCase = true) }
        }
    }

    fun filterByQuery(query: String) {
        this.query.value = query
    }

    fun toggleCategory(category: Category) {
        filter.update { it.copy(category = category) }
    }

    fun toggleProviderProtection(protection: SearchProviderProtection) {
        filter.update {
            it.copy(protection = if (it.protection == protection) null else protection)
        }
    }
}