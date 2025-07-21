package com.prajwalch.torrentsearch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.DarkTheme
import com.prajwalch.torrentsearch.data.MaxNumResults
import com.prajwalch.torrentsearch.data.SearchHistoriesRepository
import com.prajwalch.torrentsearch.data.SearchProviderId
import com.prajwalch.torrentsearch.data.SettingsRepository
import com.prajwalch.torrentsearch.providers.SearchProviders

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
    val enableNSFWMode: Boolean = false,
)

/** State for the search settings. */
data class SearchSettingsUiState(
    val hideResultsWithZeroSeeders: Boolean = false,
    val searchProviders: List<SearchProviderUiState> = emptyList(),
    val totalSearchProviders: Int = SearchProviders.namesWithId().size,
    val enabledSearchProviders: Int = 0,
    val maxNumResults: MaxNumResults = MaxNumResults.Unlimited,
    val pauseSearchHistory: Boolean = true,
)

/** State for the search providers list. */
data class SearchProviderUiState(
    val id: SearchProviderId,
    val name: String,
    val enabled: Boolean,
)

/** ViewModel that handles the business logic of Settings screen. */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val searchHistoryRepository: SearchHistoriesRepository,
) : ViewModel() {
    /** All search providers (enabled + disabled). */
    private val allSearchProviders = SearchProviders.namesWithId()

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
    private var enabledSearchProviders: Set<SearchProviderId> = emptySet()

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

    val generalSettingsUiState = settingsRepository
        .enableNSFWMode
        .map(::GeneralSettingsUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = GeneralSettingsUiState()
        )

    val searchSettingsUiState = combine(
        settingsRepository.hideResultsWithZeroSeeders,
        settingsRepository.searchProviders,
        settingsRepository.maxNumResults,
        settingsRepository.pauseSearchHistory,
    ) { hideResultsWithZeroSeeders, searchProviders, maxNumResults, pauseSearchHistory ->
        enabledSearchProviders = searchProviders

        SearchSettingsUiState(
            hideResultsWithZeroSeeders = hideResultsWithZeroSeeders,
            searchProviders = allSearchProvidersToUiStates(),
            totalSearchProviders = allSearchProviders.size,
            enabledSearchProviders = enabledSearchProviders.size,
            maxNumResults = maxNumResults,
            pauseSearchHistory = pauseSearchHistory,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SearchSettingsUiState()
    )

    /** Converts list of search provider to list of UI states. */
    private fun allSearchProvidersToUiStates(): List<SearchProviderUiState> {
        return allSearchProviders.map { (id, name) ->
            SearchProviderUiState(
                id = id,
                name = name,
                enabled = enabledSearchProviders.contains(id)
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

    /** Enables/disables NSFW mode. */
    fun updateEnableNSFWMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateEnableNSFWMode(enabled) }
    }

    /** Enables/disables an option to hide zero seeders. */
    fun updateHideResultsWithZeroSeeders(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateHideResultsWithZeroSeeders(enable)
        }
    }

    /** Enables/disables search provider associated with given id. */
    fun enableSearchProvider(providerId: SearchProviderId, enable: Boolean) {
        val updatedSearchProviders = if (enable) {
            enabledSearchProviders + providerId
        } else {
            enabledSearchProviders - providerId
        }

        viewModelScope.launch {
            settingsRepository.updateSearchProviders(providers = updatedSearchProviders)
        }
    }

    /** Updates the maximum number of results. */
    fun updateMaxNumResults(maxNumResults: MaxNumResults) {
        viewModelScope.launch { settingsRepository.updateMaxNumResults(maxNumResults) }
    }

    /** Pauses/resumes search history. */
    fun pauseSearchHistory(pause: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePauseSearchHistory(pause)
        }
    }

    /** Deletes all search history. */
    fun deleteSearchHistory() {
        viewModelScope.launch {
            searchHistoryRepository.deleteAll()
        }
    }

    companion object {
        /** Provides a factor function for [SettingsViewModel]. */
        fun provideFactory(
            settingsRepository: SettingsRepository,
            searchHistoryRepository: SearchHistoriesRepository,
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