package com.prajwalch.torrentsearch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.DarkTheme
import com.prajwalch.torrentsearch.data.repository.MaxNumResults
import com.prajwalch.torrentsearch.data.repository.SearchProvidersRepository
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.data.repository.SortCriteria
import com.prajwalch.torrentsearch.data.repository.SortOrder
import com.prajwalch.torrentsearch.models.Category

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
    val searchProvidersStat: SearchProvidersStat = SearchProvidersStat(),
    val defaultCategory: Category = Category.All,
    val defaultSortOptions: DefaultSortOptions = DefaultSortOptions(),
    val hideResultsWithZeroSeeders: Boolean = false,
    val maxNumResults: MaxNumResults = MaxNumResults.Unlimited,
)

data class SearchProvidersStat(
    val enabledSearchProvidersCount: Int = 0,
    val totalSearchProvidersCount: Int = 0,
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

/** ViewModel that handles the business logic of Settings screen. */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val searchProvidersRepository: SearchProvidersRepository,
) : ViewModel() {
    /** Information of all search providers. */
    private val allSearchProvidersInfo = searchProvidersRepository
        .searchProvidersInfo()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    /** Currently enabled search providers. */
    private val enabledSearchProvidersId = settingsRepository
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

    val generalSettingsUiState = settingsRepository
        .enableNSFWMode
        .map(::GeneralSettingsUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = GeneralSettingsUiState()
        )

    private val searchProvidersStatFlow = combine(
        settingsRepository.enabledSearchProvidersId.map { it.size },
        searchProvidersRepository.count(),
        ::SearchProvidersStat
    )

    private val defaultSortOptionsFlow = combine(
        settingsRepository.defaultSortCriteria,
        settingsRepository.defaultSortOrder,
        ::DefaultSortOptions
    )

    val searchSettingsUiState = combine(
        searchProvidersStatFlow,
        settingsRepository.defaultCategory,
        defaultSortOptionsFlow,
        settingsRepository.hideResultsWithZeroSeeders,
        settingsRepository.maxNumResults,
        ::SearchSettingsUiState,
    ).stateIn(
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

    /** Enables/disables dynamic theme. */
    fun enableDynamicTheme(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateEnableDynamicTheme(enable = enable)
        }
    }

    /** Changes the dark theme mode. */
    fun changeDarkTheme(darkTheme: DarkTheme) {
        viewModelScope.launch {
            settingsRepository.updateDarkTheme(darkTheme = darkTheme)
        }
    }

    /** Enables/disables pure black mode. */
    fun enablePureBlackTheme(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePureBlack(enable = enable)
        }
    }

    /** Changes the default category to given one. */
    fun changeDefaultCategory(category: Category) {
        viewModelScope.launch {
            settingsRepository.updateDefaultCategory(category = category)
        }
    }

    /** Enables/disables NSFW mode. */
    fun enableNSFWMode(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateEnableNSFWMode(enable = enable)
            if (!enable) disableRestrictedSearchProviders()
        }
    }

    /** Disables NSFW and Unsafe search providers which are currently enabled. */
    private suspend fun disableRestrictedSearchProviders() {
        val newEnabledSearchProvidersId = allSearchProvidersInfo
            .value
            .filter { it.id in enabledSearchProvidersId.value }
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

    /** Changes the default sort criteria. */
    fun changeDefaultSortCriteria(sortCriteria: SortCriteria) {
        viewModelScope.launch {
            settingsRepository.updateDefaultSortCriteria(sortCriteria = sortCriteria)
        }
    }

    /** Changes the default sort order. */
    fun changeDefaultSortOrder(sortOrder: SortOrder) {
        viewModelScope.launch {
            settingsRepository.updateDefaultSortOrder(sortOrder = sortOrder)
        }
    }

    /** Enables/disables an option to hide zero seeders. */
    fun hideResultsWithZeroSeeders(yes: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateHideResultsWithZeroSeeders(enable = yes)
        }
    }

    /** Updates the maximum number of results. */
    fun updateMaxNumResults(maxNumResults: MaxNumResults) {
        viewModelScope.launch {
            settingsRepository.updateMaxNumResults(maxNumResults = maxNumResults)
        }
    }

    /** Saves/unsaves search history. */
    fun saveSearchHistory(save: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSaveSearchHistory(save = save)
        }
    }

    /** Shows/hides search history. */
    fun showSearchHistory(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowSearchHistory(show = show)
        }
    }

    companion object {
        /** Provides a factory function for [SettingsViewModel]. */
        fun provideFactory(
            settingsRepository: SettingsRepository,
            searchProvidersRepository: SearchProvidersRepository,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(
                        settingsRepository = settingsRepository,
                        searchProvidersRepository = searchProvidersRepository,
                    ) as T
                }
            }
        }
    }
}