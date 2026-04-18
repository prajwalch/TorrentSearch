package com.prajwalch.torrentsearch.ui.settings.searchproviders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prajwalch.torrentsearch.data.repository.SettingsRepository

import com.prajwalch.torrentsearch.domain.SearchProviderInfoItem
import com.prajwalch.torrentsearch.domain.SearchProvidersManager
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.providers.SearchProviderSafetyStatus
import com.prajwalch.torrentsearch.providers.SearchProviderType

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

data class SearchProvidersUiState(
    val selectedCategory: Category? = null,
    val searchProviders: List<SearchProviderListItem> = emptyList(),
    val totalNumProviders: Int = 0,
    val enabledProvidersCount: Int = 0,
)

data class SearchProviderListItem(
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
    private val searchProvidersManager: SearchProvidersManager,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val selectedCategory = MutableStateFlow<Category?>(null)

    val uiState = combine(
        selectedCategory,
        searchProvidersManager.getProviderInfos(),
        searchProvidersManager.getProvidersCount(),
        settingsRepository.enabledSearchProvidersId.map { it.size },
        ::createUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = SearchProvidersUiState(),
    )

    private fun createUiState(
        selectedCategory: Category?,
        searchProvidersInfo: List<SearchProviderInfoItem>,
        totalNumProviders: Int,
        enabledProvidersCount: Int,
    ): SearchProvidersUiState {
        val searchProviders = searchProvidersInfo
            .filter { selectedCategory == null || it.specializedCategory == selectedCategory }
            .map {
                SearchProviderListItem(
                    id = it.id,
                    name = it.name,
                    url = it.url,
                    specializedCategory = it.specializedCategory,
                    safetyStatus = it.safetyStatus,
                    type = it.type,
                    enabled = it.isEnabled,
                )
            }

        return SearchProvidersUiState(
            selectedCategory = selectedCategory,
            searchProviders = searchProviders,
            totalNumProviders = totalNumProviders,
            enabledProvidersCount = enabledProvidersCount,
        )
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
            searchProvidersManager.enableAllProviders()
        }
    }

    /** Disables all search providers. */
    fun disableAllSearchProviders() {
        viewModelScope.launch {
            searchProvidersManager.disableAllProviders()
        }
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
        if (selectedCategory.value == category) {
            selectedCategory.value = null
        } else {
            selectedCategory.value = category
        }
    }
}