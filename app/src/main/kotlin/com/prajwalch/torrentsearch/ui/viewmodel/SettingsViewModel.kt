package com.prajwalch.torrentsearch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.DarkTheme
import com.prajwalch.torrentsearch.data.MaxNumResults
import com.prajwalch.torrentsearch.data.SearchHistoryRepository
import com.prajwalch.torrentsearch.data.SettingsRepository
import com.prajwalch.torrentsearch.domain.SortCriteria
import com.prajwalch.torrentsearch.domain.SortOrder
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.providers.SearchProviderSafetyStatus
import com.prajwalch.torrentsearch.providers.SearchProviders

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** State for the appearance settings. */
data class AppearanceSettingsUiState(
    val enableDynamicTheme: Boolean = true,
    val darkTheme: DarkTheme = DarkTheme.FollowSystem,
    val pureBlack: Boolean = false,
)

/** State for the general settings. */
data class GeneralSettingsUiState(
    val defaultCategory: Category = Category.All,
    val enableNSFWMode: Boolean = false,
)

/** State for the search settings. */
data class SearchSettingsUiState(
    val searchProviders: List<SearchProviderUiState> = emptyList(),
    val defaultSortOptions: DefaultSortOptions = DefaultSortOptions(),
    val hideResultsWithZeroSeeders: Boolean = false,
    val maxNumResults: MaxNumResults = MaxNumResults.Unlimited,
    val totalSearchProviders: Int = SearchProviders.count(),
    val enabledSearchProviders: Int = 0,
)

data class DefaultSortOptions(
    val sortCriteria: SortCriteria = SortCriteria.Default,
    val sortOrder: SortOrder = SortOrder.Default,
)

/** State for the search history settings. */
data class SearchHistorySettingsUiState(
    val saveSearchHistory: Boolean = true,
    val showSearchHistory: Boolean = true,
)

/** State for the search providers list. */
data class SearchProviderUiState(
    val id: SearchProviderId,
    val name: String,
    val url: String,
    val specializedCategory: Category,
    val safetyStatus: SearchProviderSafetyStatus,
    val enabled: Boolean,
)

/** ViewModel that handles the business logic of Settings screen. */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
) : ViewModel() {
    /** Information of all search providers. */
    private val allSearchProvidersInfo = SearchProviders.allInfo()

    /**
     * Currently enabled search providers.
     *
     * Settings screen receives all the providers with enable/disable
     * state instead of only enabled ones and reports state change event
     * for only one at a time through [enableSearchProvider].
     *
     * Only then we will create a set of enabled providers and pass them to
     * repository like how it expects.
     */
    private var enabledSearchProvidersId = settingsRepository
        .enabledSearchProvidersId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptySet(),
        )

    val appearanceSettingsUiState = combine(
        settingsRepository.enableDynamicTheme,
        settingsRepository.darkTheme,
        settingsRepository.pureBlack,
        ::AppearanceSettingsUiState
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppearanceSettingsUiState()
    )

    val generalSettingsUiState = combine(
        settingsRepository.defaultCategory,
        settingsRepository.enableNSFWMode,
        ::GeneralSettingsUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = GeneralSettingsUiState()
    )

    val searchSettingsUiState = combine(
        settingsRepository.defaultSortCriteria,
        settingsRepository.defaultSortOrder,
        settingsRepository.hideResultsWithZeroSeeders,
        settingsRepository.maxNumResults,
    ) { defaultSortCriteria, defaultSortOrder, hideResultsWithZeroSeeders, maxNumResults ->
        SearchSettingsUiState(
            searchProviders = allSearchProvidersInfoToUiStates(),
            defaultSortOptions = DefaultSortOptions(
                sortCriteria = defaultSortCriteria,
                sortOrder = defaultSortOrder,
            ),
            hideResultsWithZeroSeeders = hideResultsWithZeroSeeders,
            maxNumResults = maxNumResults,
            enabledSearchProviders = enabledSearchProvidersId.value.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SearchSettingsUiState()
    )

    val searchHistorySettingsUiState = combine(
        settingsRepository.saveSearchHistory,
        settingsRepository.showSearchHistory,
        ::SearchHistorySettingsUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SearchHistorySettingsUiState()
    )

    /** Converts list of search providers info to list of UI states. */
    private fun allSearchProvidersInfoToUiStates(): List<SearchProviderUiState> {
        return allSearchProvidersInfo.map { info ->
            SearchProviderUiState(
                id = info.id,
                name = info.name,
                url = info.url.removePrefix("https://"),
                specializedCategory = info.specializedCategory,
                safetyStatus = info.safetyStatus,
                enabled = enabledSearchProvidersId.value.contains(info.id)
            )
        }
    }

    /** Enables/disables dynamic theme. */
    fun updateEnableDynamicTheme(enable: Boolean) {
        viewModelScope.launch { settingsRepository.updateEnableDynamicTheme(enable) }
    }

    /** Changes the dark theme mode. */
    fun updateDarkTheme(darkTheme: DarkTheme) {
        viewModelScope.launch { settingsRepository.updateDarkTheme(darkTheme) }
    }

    /** Enables/disables pure black mode. */
    fun updatePureBlack(enable: Boolean) {
        viewModelScope.launch { settingsRepository.updatePureBlack(enable) }
    }

    /** Changes the default category to given one. */
    fun updateDefaultCategory(category: Category) {
        viewModelScope.launch { settingsRepository.updateDefaultCategory(category) }
    }

    /** Enables/disables NSFW mode. */
    fun updateEnableNSFWMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateEnableNSFWMode(enabled)
            if (!enabled) disableRestrictedSearchProviders()
        }
    }

    /** Disables NSFW and Unsafe search providers which are currently enabled. */
    private suspend fun disableRestrictedSearchProviders() {
        val newEnabledSearchProvidersId = allSearchProvidersInfo
            .filter { enabledSearchProvidersId.value.contains(it.id) }
            .filter { !it.specializedCategory.isNSFW && !it.safetyStatus.isUnsafe() }
            .map { it.id }
            .toSet()

        if (newEnabledSearchProvidersId.isEmpty()) {
            return
        }

        if (newEnabledSearchProvidersId != enabledSearchProvidersId.value) {
            settingsRepository.updateEnabledSearchProvidersId(
                providersId = newEnabledSearchProvidersId.toSet(),
            )
        }
    }

    /** Enables/disables search provider matching the specified ID. */
    fun enableSearchProvider(providerId: SearchProviderId, enable: Boolean) {
        val newEnabledSearchProvidersId = if (enable) {
            enabledSearchProvidersId.value + providerId
        } else {
            enabledSearchProvidersId.value - providerId
        }

        viewModelScope.launch {
            settingsRepository.updateEnabledSearchProvidersId(
                providersId = newEnabledSearchProvidersId,
            )
        }
    }

    /** Enables all search providers. */
    fun enableAllSearchProviders() {
        val allSearchProvidersId = allSearchProvidersInfo.map { it.id }.toSet()

        viewModelScope.launch {
            settingsRepository.updateEnabledSearchProvidersId(
                providersId = allSearchProvidersId,
            )
        }
    }

    /** Disables all search providers. */
    fun disableAllSearchProviders() {
        viewModelScope.launch {
            settingsRepository.updateEnabledSearchProvidersId(
                providersId = emptySet(),
            )
        }
    }

    /** Resets enabled search providers to default. */
    fun resetEnabledSearchProvidersToDefault() {
        viewModelScope.launch {
            settingsRepository.updateEnabledSearchProvidersId(
                providersId = SearchProviders.defaultEnabledIds(),
            )
        }
    }

    /** Changes the default sort criteria. */
    fun updateDefaultSortCriteria(sortCriteria: SortCriteria) {
        viewModelScope.launch {
            settingsRepository.updateDefaultSortCriteria(sortCriteria = sortCriteria)
        }
    }

    /** Changes the default sort order. */
    fun updateDefaultSortOrder(sortOrder: SortOrder) {
        viewModelScope.launch {
            settingsRepository.updateDefaultSortOrder(sortOrder = sortOrder)
        }
    }

    /** Enables/disables an option to hide zero seeders. */
    fun updateHideResultsWithZeroSeeders(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateHideResultsWithZeroSeeders(enable)
        }
    }

    /** Updates the maximum number of results. */
    fun updateMaxNumResults(maxNumResults: MaxNumResults) {
        viewModelScope.launch { settingsRepository.updateMaxNumResults(maxNumResults) }
    }

    /** Saves/unsaves search history. */
    fun saveSearchHistory(save: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSaveSearchHistory(save)
        }
    }

    /** Shows/hides search history. */
    fun showSearchHistory(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowSearchHistory(show = show)
        }
    }

    /** Clears all search history. */
    fun clearSearchHistory() {
        viewModelScope.launch {
            searchHistoryRepository.clearAll()
        }
    }

    companion object {
        /** Provides a factor function for [SettingsViewModel]. */
        fun provideFactory(
            settingsRepository: SettingsRepository,
            searchHistoryRepository: SearchHistoryRepository,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(
                        settingsRepository = settingsRepository,
                        searchHistoryRepository = searchHistoryRepository,
                    ) as T
                }
            }
        }
    }
}