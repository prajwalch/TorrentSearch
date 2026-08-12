package com.prajwalch.torrentsearch.ui.settings.searchproviders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.domain.SearchProvidersManager
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.CloudflareProtectionStatus
import com.prajwalch.torrentsearch.domain.model.SearchProviderInfo
import com.prajwalch.torrentsearch.providers.SearchProviderId

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

data class SearchProvidersUiState(
    val filter: SearchProviderFilter = SearchProviderFilter(),
    val searchProviders: List<SearchProviderInfo> = emptyList(),
    val totalNumProviders: Int = 0,
    val enabledProvidersCount: Int = 0,
    val protectionUpdateState: ProtectionUpdateState = ProtectionUpdateState.Idle,
)

data class SearchProviderFilter(
    val category: Category = Category.All,
    val protection: SearchProviderProtection? = null,
) {
    val isEmpty: Boolean
        get() = category == Category.All && protection == null
}

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
    private val searchProvidersManager: SearchProvidersManager,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(SearchProviderFilter())
    private val protectionUpdateState =
        MutableStateFlow<ProtectionUpdateState>(ProtectionUpdateState.Idle)

    val uiState = combine(
        filter,
        protectionUpdateState,
        searchProvidersManager.getProviderInfos(),
        searchProvidersManager.getProvidersCount(),
        settingsRepository.enabledSearchProviderIds.map { it.size },
    ) {
            filter,
            protectionUpdateState,
            searchProviderInfos,
            totalNumProviders,
            enabledProvidersCount,
        ->

        val filteredSearchProviderInfos =
            filterSearchProviderInfos(
                infos = searchProviderInfos,
                filter = filter,
            )
        SearchProvidersUiState(
            filter = filter,
            searchProviders = filteredSearchProviderInfos,
            totalNumProviders = totalNumProviders,
            enabledProvidersCount = enabledProvidersCount,
            protectionUpdateState = protectionUpdateState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = SearchProvidersUiState(),
    )

    private fun filterSearchProviderInfos(
        infos: List<SearchProviderInfo>,
        filter: SearchProviderFilter,
    ): List<SearchProviderInfo> {
        val predicates = buildFilterPredicates(filter)
        return infos.filter { info ->
            predicates.all { predicate -> predicate(info) }
        }
    }

    private fun buildFilterPredicates(
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
    }

    /** Enables/disables search provider matching the specified ID. */
    fun enableSearchProvider(providerId: SearchProviderId, enable: Boolean) {
        viewModelScope.launch {
            if (enable) {
                searchProvidersManager.enableProvider(providerId)
            } else {
                searchProvidersManager.disableProvider(providerId)
            }
        }
    }

    /** Enables all search providers. */
    fun enableAllSearchProviders() {
        viewModelScope.launch {
            // If filter is not applied, enable all.
            if (filter.value.isEmpty) {
                searchProvidersManager.enableAllProviders()
                return@launch
            }

            // If filter is applied, only enable those that're currently displayed.
            val providerIds = uiState.value.searchProviders.map { it.id }.toSet()
            searchProvidersManager.enableProviderByIds(providerIds)
        }
    }

    /** Disables all search providers. */
    fun disableAllSearchProviders() {
        viewModelScope.launch {
            // If filter is not applied, disable all.
            if (filter.value.isEmpty) {
                searchProvidersManager.disableAllProviders()
                return@launch
            }

            // If filter is applied, only disable those that're currently displayed.
            val providerIds = uiState.value.searchProviders.map { it.id }.toSet()
            searchProvidersManager.disableProviderByIds(providerIds)
        }
    }

    fun updateProtectionStatus() {
        protectionUpdateState.value = ProtectionUpdateState.Updating

        viewModelScope.launch {
            val result = searchProvidersManager.updateProvidersProtectionStatus()
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
            searchProvidersManager.resetToDefault()
        }
    }

    /** Deletes the Torznab search provider that matches the specified ID. */
    fun deleteTorznabConfig(id: String) {
        viewModelScope.launch {
            searchProvidersManager.deleteTorznabConfig(id)
        }
    }

    /** Selects/unselects the given category. */
    fun toggleCategory(category: Category) {
        filter.update { it.copy(category = category) }
    }

    fun toggleProviderProtection(protection: SearchProviderProtection) {
        filter.update {
            it.copy(protection = if (it.protection == protection) null else protection)
        }
    }

    fun markProviderAsUnlocked(id: SearchProviderId) {
        viewModelScope.launch {
            searchProvidersManager.unlockProvider(id)
        }
    }
}