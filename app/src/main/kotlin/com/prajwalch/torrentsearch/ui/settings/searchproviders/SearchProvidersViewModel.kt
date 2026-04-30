package com.prajwalch.torrentsearch.ui.settings.searchproviders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.domain.SearchProvidersManager
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.SearchProviderInfo
import com.prajwalch.torrentsearch.providers.SearchProviderId

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
    val searchProviders: List<SearchProviderInfo> = emptyList(),
    val totalNumProviders: Int = 0,
    val enabledProvidersCount: Int = 0,
)

/** ViewModel which handles the business logic of Search providers screen. */
@HiltViewModel
class SearchProvidersViewModel @Inject constructor(
    private val searchProvidersManager: SearchProvidersManager,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val selectedCategory = MutableStateFlow<Category?>(null)

    private val searchProviderInfos =
        combine(
            selectedCategory,
            searchProvidersManager.getProviderInfos()
        ) { filterCategory, infos ->
            infos.filter { filterCategory == null || it.specializedCategory == filterCategory }
        }

    val uiState = combine(
        selectedCategory,
        searchProviderInfos,
        searchProvidersManager.getProvidersCount(),
        settingsRepository.enabledSearchProvidersId.map { it.size },
    ) { selectedCategory, searchProviderInfos, totalNumProviders, enabledProvidersCount ->
        SearchProvidersUiState(
            selectedCategory = selectedCategory,
            searchProviders = searchProviderInfos,
            totalNumProviders = totalNumProviders,
            enabledProvidersCount = enabledProvidersCount,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = SearchProvidersUiState(),
    )

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