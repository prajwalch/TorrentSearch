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

data class AppearanceSettingsUiState(
    val enableDynamicTheme: Boolean = true,
    val darkTheme: DarkTheme = DarkTheme.FollowSystem,
)

data class SearchSettingsUiState(
    val enableNSFWSearch: Boolean = false,
    val searchProviders1: List<SearchProviderUiState> = emptyList(),
    val totalSearchProviders: Int = SearchProviders.namesWithId().size,
    val enabledSearchProviders: Int = 0,
)

data class SearchProviderUiState(
    val id: SearchProviderId,
    val name: String,
    val enabled: Boolean,
)

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    private val allSearchProviders = SearchProviders.namesWithId()
    private var enabledSearchProviders: Set<SearchProviderId> = emptySet()

    val appearanceSettings = combine(
        repository.enableDynamicTheme,
        repository.darkTheme,
        ::AppearanceSettingsUiState
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppearanceSettingsUiState()
    )

    val searchSettings = combine(
        repository.enableNSFWSearch,
        repository.searchProviders
    ) { enableNSFWSearch, searchProviders ->
        enabledSearchProviders = searchProviders

        SearchSettingsUiState(
            enableNSFWSearch = enableNSFWSearch,
            searchProviders1 = createSearchProviderStates(),
            totalSearchProviders = allSearchProviders.size,
            enabledSearchProviders = enabledSearchProviders.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SearchSettingsUiState()
    )

    private fun createSearchProviderStates(): List<SearchProviderUiState> {
        return allSearchProviders.map { (id, name) ->
            SearchProviderUiState(
                id = id,
                name = name,
                enabled = enabledSearchProviders.contains(id)
            )
        }
    }

    fun updateEnableDynamicTheme(enabled: Boolean) {
        viewModelScope.launch { repository.updateEnableDynamicTheme(enabled) }
    }

    fun updateDarkTheme(darkTheme: DarkTheme) {
        viewModelScope.launch { repository.updateDarkTheme(darkTheme) }
    }

    fun updateEnableNSFWSearch(enabled: Boolean) {
        viewModelScope.launch { repository.updateEnableNSFWSearch(enabled) }
    }

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
}

class SettingsViewModelFactory(private val settingsRepository: SettingsRepository) :
    ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(repository = settingsRepository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}