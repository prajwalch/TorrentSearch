package com.prajwalch.torrentsearch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.DarkTheme
import com.prajwalch.torrentsearch.data.SearchProviderId
import com.prajwalch.torrentsearch.data.SettingsRepository
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

/** State for the search settings. */
data class SearchSettingsUiState(
    val enableNSFWSearch: Boolean = false,
    val hideResultsWithZeroSeeders: Boolean = false,
    val searchProviders: List<SearchProviderUiState> = emptyList(),
    val totalSearchProviders: Int = SearchProviders.namesWithId().size,
    val enabledSearchProviders: Int = 0,
)

/** State for the search providers list. */
data class SearchProviderUiState(
    val id: SearchProviderId,
    val name: String,
    val enabled: Boolean,
)

/** ViewModel that handles the business logic of Settings screen. */
class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
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
        repository.enableDynamicTheme,
        repository.darkTheme,
        repository.pureBlack,
        ::AppearanceSettingsUiState
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppearanceSettingsUiState()
    )

    val searchSettingsUiState = combine(
        repository.enableNSFWSearch,
        repository.hideResultsWithZeroSeeders,
        repository.searchProviders,
    ) { enableNSFWSearch, hideResultsWithZeroSeeders, searchProviders ->
        enabledSearchProviders = searchProviders

        SearchSettingsUiState(
            enableNSFWSearch = enableNSFWSearch,
            hideResultsWithZeroSeeders = hideResultsWithZeroSeeders,
            searchProviders = allSearchProvidersToUiStates(),
            totalSearchProviders = allSearchProviders.size,
            enabledSearchProviders = enabledSearchProviders.size,
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
        viewModelScope.launch { repository.updateEnableDynamicTheme(enable) }
    }

    /** Changes the dark theme mode. */
    fun updateDarkTheme(darkTheme: DarkTheme) {
        viewModelScope.launch { repository.updateDarkTheme(darkTheme) }
    }

    /** Enables/disables pure black mode. */
    fun updatePureBlack(enable: Boolean) {
        viewModelScope.launch { repository.updatePureBlack(enable) }
    }

    /** Enables/disables NSFW search. */
    fun updateEnableNSFWSearch(enabled: Boolean) {
        viewModelScope.launch { repository.updateEnableNSFWSearch(enabled) }
    }

    /** Enables/disables an option to hide zero seeders. */
    fun updateHideResultsWithZeroSeeders(enable: Boolean) {
        viewModelScope.launch {
            repository.updateHideResultsWithZeroSeeders(enable)
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
            repository.updateSearchProviders(providers = updatedSearchProviders)
        }
    }

    companion object {
        /** Provides a factor function for [SettingsViewModel]. */
        fun provideFactory(settingsRepository: SettingsRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(repository = settingsRepository) as T
                }
            }
        }
    }
}