package com.prajwalch.torrentsearch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.SettingsRepository
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.providers.SearchProviderSafetyStatus
import com.prajwalch.torrentsearch.providers.SearchProviders

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** State for the single search provider. */
data class SearchProviderUiState(
    val id: SearchProviderId,
    val name: String,
    val url: String,
    val specializedCategory: Category,
    val safetyStatus: SearchProviderSafetyStatus,
    val enabled: Boolean,
)

/** ViewModel which handles the business logic of Search providers screen. */
class SearchProvidersViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
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
    private val enabledSearchProvidersId = settingsRepository
        .enabledSearchProvidersId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptySet(),
        )

    val searchProvidersUiState = settingsRepository
        .enabledSearchProvidersId
        .map { createSearchProvidersUiState(enabledSearchProvidersId = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Converts list of search providers info to list of UI states. */
    private fun createSearchProvidersUiState(
        enabledSearchProvidersId: Set<SearchProviderId>,
    ): List<SearchProviderUiState> = allSearchProvidersInfo.map {
        SearchProviderUiState(
            id = it.id,
            name = it.name,
            url = it.url,
            specializedCategory = it.specializedCategory,
            safetyStatus = it.safetyStatus,
            enabled = it.id in enabledSearchProvidersId
        )
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

    companion object {
        /** Provides a factory function for [SearchProvidersViewModel]. */
        fun provideFactory(settingsRepository: SettingsRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SearchProvidersViewModel(settingsRepository = settingsRepository) as T
                }
            }
        }
    }
}