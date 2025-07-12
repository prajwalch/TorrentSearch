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

data class AppearanceSettings(
    val enableDynamicTheme: Boolean = true,
    val darkTheme: DarkTheme = DarkTheme.FollowSystem,
)

data class SearchSettings(
    val enableNSFWSearch: Boolean = false,
    val searchProviders: Set<SearchProviderId> = SearchProviders.ids(),
)

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val appearanceSettings = combine(
        repository.enableDynamicTheme,
        repository.darkTheme,
        ::AppearanceSettings
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppearanceSettings()
    )

    val searchSettings = combine(
        repository.enableNSFWSearch,
        repository.searchProviders,
        ::SearchSettings
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchSettings()
    )

    fun updateEnableDynamicTheme(enabled: Boolean) {
        viewModelScope.launch { repository.updateEnableDynamicTheme(enabled) }
    }

    fun updateDarkTheme(darkTheme: DarkTheme) {
        viewModelScope.launch { repository.updateDarkTheme(darkTheme) }
    }

    fun updateEnableNSFWSearch(enabled: Boolean) {
        viewModelScope.launch { repository.updateEnableNSFWSearch(enabled) }
    }

    fun updateSearchProviders(providers: Set<SearchProviderId>) {
        viewModelScope.launch { repository.updateSearchProviders(providers) }
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