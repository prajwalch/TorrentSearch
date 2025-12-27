package com.prajwalch.torrentsearch.ui.settings.searchproviders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SearchProvidersRepository
import com.prajwalch.torrentsearch.data.repository.SettingsRepository
import com.prajwalch.torrentsearch.domain.models.Category
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.providers.SearchProviderInfo
import com.prajwalch.torrentsearch.providers.SearchProviderSafetyStatus
import com.prajwalch.torrentsearch.providers.SearchProviderType

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

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
        searchProvidersRepository.observeSearchProvidersInfo(),
        settingsRepository.enabledSearchProvidersId,
    ) { searchProvidersInfo, enabledSearchProvidersId ->
        createSearchProvidersUiState(
            searchProvidersInfo = searchProvidersInfo,
            enabledSearchProvidersId = enabledSearchProvidersId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = emptyList(),
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
        viewModelScope.launch {
            val enabledSearchProvidersId = settingsRepository
                .enabledSearchProvidersId
                .firstOrNull()
                .orEmpty()

            val newEnabledSearchProvidersId = if (enable) {
                enabledSearchProvidersId + providerId
            } else {
                enabledSearchProvidersId - providerId
            }

            settingsRepository.setEnabledSearchProvidersId(
                providersId = newEnabledSearchProvidersId,
            )
        }
    }

    /** Enables all search providers. */
    fun enableAllSearchProviders() {
        viewModelScope.launch {
            val allSearchProvidersId = searchProvidersRepository
                .observeSearchProvidersInfo()
                .map { infos -> infos.map { it.id } }
                .firstOrNull()
                ?: return@launch

            settingsRepository.setEnabledSearchProvidersId(
                providersId = allSearchProvidersId.toSet(),
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
                providersId = searchProvidersRepository.getEnabledSearchProvidersId(),
            )
        }
    }

    /** Deletes the Torznab search provider that matches the specified ID. */
    fun deleteTorznabConfig(id: String) {
        viewModelScope.launch {
            searchProvidersRepository.deleteTorznabConfig(id = id)
        }
        enableSearchProvider(providerId = id, enable = false)
    }
}