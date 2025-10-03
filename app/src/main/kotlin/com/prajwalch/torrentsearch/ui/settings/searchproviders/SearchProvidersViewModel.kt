package com.prajwalch.torrentsearch.ui.settings.searchproviders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SearchProvidersRepository
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.providers.SearchProviderInfo
import com.prajwalch.torrentsearch.providers.SearchProviderSafetyStatus
import com.prajwalch.torrentsearch.providers.SearchProviderType

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import javax.inject.Inject

/** State for the single search provider. */
data class SearchProviderUiState(
    val id: SearchProviderId,
    val name: String,
    val url: String,
    val specializedCategory: Category,
    val safetyStatus: SearchProviderSafetyStatus,
    val type: SearchProviderType,
    val enabled: Boolean,
)

/** ViewModel which handles the business logic of Search providers screen. */
@HiltViewModel
class SearchProvidersViewModel @Inject constructor(
    private val searchProvidersRepository: SearchProvidersRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val uiState = combine(
        settingsRepository.enabledSearchProvidersId,
        searchProvidersRepository.searchProvidersInfo(),
    ) { enabledSearchProvidersId, searchProvidersInfo ->
        createSearchProvidersUiState(
            searchProvidersInfo = searchProvidersInfo,
            enabledSearchProvidersId = enabledSearchProvidersId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )


    /** Information of all search providers. */
    private val allSearchProvidersInfo = searchProvidersRepository
        .searchProvidersInfo()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

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

    /** Converts list of search providers info to list of UI states. */
    private fun createSearchProvidersUiState(
        enabledSearchProvidersId: Set<SearchProviderId>,
        searchProvidersInfo: List<SearchProviderInfo>,
    ): List<SearchProviderUiState> = searchProvidersInfo.map {
        SearchProviderUiState(
            id = it.id,
            name = it.name,
            url = it.url,
            specializedCategory = it.specializedCategory,
            safetyStatus = it.safetyStatus,
            type = it.type,
            enabled = it.id in enabledSearchProvidersId,
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
            settingsRepository.setEnabledSearchProvidersId(
                providersId = newEnabledSearchProvidersId,
            )
        }
    }

    /** Enables all search providers. */
    fun enableAllSearchProviders() {
        val allSearchProvidersId = allSearchProvidersInfo.value.map { it.id }.toSet()

        viewModelScope.launch {
            settingsRepository.setEnabledSearchProvidersId(
                providersId = allSearchProvidersId,
            )
        }
    }

    /** Disables all search providers. */
    fun disableAllSearchProviders() {
        viewModelScope.launch {
            settingsRepository.setEnabledSearchProvidersId(
                providersId = emptySet(),
            )
        }
    }

    /** Resets enabled search providers to default. */
    fun resetEnabledSearchProvidersToDefault() {
        viewModelScope.launch {
            settingsRepository.setEnabledSearchProvidersId(
                providersId = searchProvidersRepository.defaultEnabledIds(),
            )
        }
    }

    /** Deletes the Torznab search provider that matches the specified ID. */
    fun deleteTorznabSearchProvider(id: String) {
        viewModelScope.launch {
            searchProvidersRepository.deleteTorznabSearchProvider(id = id)
        }
        enableSearchProvider(providerId = id, enable = false)
    }
}