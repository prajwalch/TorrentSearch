package com.prajwalch.torrentsearch.ui.settings.searchproviders

import android.util.Patterns

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SearchProvidersRepository
import com.prajwalch.torrentsearch.models.Category
import com.prajwalch.torrentsearch.providers.SearchProviderSafetyStatus
import com.prajwalch.torrentsearch.providers.TorznabSearchProviderConfig

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import javax.inject.Inject

data class TorznabSearchProviderConfigUiState(
    val config: TorznabSearchProviderConfig =
        TorznabSearchProviderConfig(
            id = "",
            name = "",
            url = "",
            apiKey = "",
        ),
    val isUrlValid: Boolean = true,
    val isConfigSaved: Boolean = false,
) {
    fun isConfigNotBlank() =
        config.name.isNotBlank() && config.url.isNotBlank() && config.apiKey.isNotBlank()
}

@HiltViewModel
class TorznabSearchProviderConfigViewModel @Inject constructor(
    private val searchProvidersRepository: SearchProvidersRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val torznabSearchProviderId = savedStateHandle.get<String>("id")

    private val _uiState2 = MutableStateFlow(TorznabSearchProviderConfigUiState())
    val uiState2 = _uiState2.asStateFlow()

    init {
        loadExistingConfig()
    }

    private fun loadExistingConfig() = viewModelScope.launch {
        val id = torznabSearchProviderId ?: return@launch
        val existingConfig = searchProvidersRepository
            .findTorznabSearchProviderConfig(id = id)
            ?: return@launch

        _uiState2.update { it.copy(config = existingConfig) }
    }

    fun changeName(name: String) {
        _uiState2.update {
            it.copy(config = it.config.copy(name = name))
        }
    }

    fun changeUrl(url: String) {
        _uiState2.update {
            it.copy(config = it.config.copy(url = url))
        }
    }

    fun changeAPIKey(apiKey: String) {
        _uiState2.update {
            it.copy(config = it.config.copy(apiKey = apiKey))
        }
    }

    fun changeCategory(category: Category) {
        _uiState2.update {
            it.copy(config = it.config.copy(category = category))
        }
    }

    fun changeSafetyStatus(safetyStatus: SearchProviderSafetyStatus) {
        _uiState2.update {
            it.copy(config = it.config.copy(safetyStatus = safetyStatus))
        }
    }

    fun saveConfig() {
        val urlPatternMatcher = Patterns.WEB_URL.matcher(_uiState2.value.config.url)

        if (!urlPatternMatcher.matches()) {
            _uiState2.update {
                it.copy(isUrlValid = false, isConfigSaved = false)
            }
            return
        }

        viewModelScope.launch {
            val isConfigNew = torznabSearchProviderId == null

            if (isConfigNew) {
                searchProvidersRepository.addTorznabSearchProvider(
                    config = _uiState2.value.config,
                )
            } else {
                searchProvidersRepository.updateTorznabSearchProvider(
                    config = _uiState2.value.config,
                )
            }

            _uiState2.update {
                it.copy(isUrlValid = true, isConfigSaved = true)
            }
        }
    }
}