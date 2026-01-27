package com.prajwalch.torrentsearch.ui.settings.searchproviders.addedit

import android.util.Patterns

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SearchProvidersRepository
import com.prajwalch.torrentsearch.domain.models.Category
import com.prajwalch.torrentsearch.domain.models.TorznabConnectionCheckResult
import com.prajwalch.torrentsearch.providers.SearchProviderId

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import javax.inject.Inject

data class TorznabConfigUiState(
    val searchProviderName: String = "",
    val url: String = "",
    val apiKey: String = "",
    val category: Category = Category.All,
    val isNewConfig: Boolean = true,
    val isUrlValid: Boolean = true,
    val isConfigSaved: Boolean = false,
    val isConnectionCheckRunning: Boolean = false,
    val connectionCheckResult: TorznabConnectionCheckResult? = null,
) {
    fun isConfigNotBlank() =
        searchProviderName.isNotBlank() && url.isNotBlank() && apiKey.isNotBlank()
}

@HiltViewModel
class TorznabConfigViewModel @Inject constructor(
    private val searchProvidersRepository: SearchProvidersRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    /**
     * ID of the search provider whose config to edit (edit mode).
     *
     * if `null`, a new config is created (add mode).
     */
    private val searchProviderId = savedStateHandle.get<String>("id")

    private val _uiState = MutableStateFlow(
        TorznabConfigUiState(isNewConfig = searchProviderId == null),
    )
    val uiState = _uiState.asStateFlow()

    init {
        searchProviderId?.let(::loadConfig)
    }

    private fun loadConfig(id: SearchProviderId) = viewModelScope.launch {
        val existingConfig = searchProvidersRepository.findTorznabConfig(id = id) ?: return@launch

        _uiState.update {
            it.copy(
                searchProviderName = existingConfig.searchProviderName,
                url = existingConfig.url,
                apiKey = existingConfig.apiKey,
                category = existingConfig.category,
            )
        }
    }

    fun setSearchProviderName(name: String) {
        _uiState.update { it.copy(searchProviderName = name) }
    }

    fun setUrl(url: String) {
        _uiState.update { it.copy(url = url) }
    }

    fun setAPIKey(apiKey: String) {
        _uiState.update { it.copy(apiKey = apiKey) }
    }

    fun setCategory(category: Category) {
        _uiState.update { it.copy(category = category) }
    }

    fun checkConnection() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isConnectionCheckRunning = false,
                    connectionCheckResult = null,
                )
            }

            if (!isUrlValid()) {
                _uiState.update { it.copy(isUrlValid = false) }
                return@launch
            }

            _uiState.update { it.copy(isConnectionCheckRunning = true) }

            val url = _uiState.value.url
            val apiKey = _uiState.value.apiKey
            val connectionCheckResult =
                searchProvidersRepository.checkTorznabConnection(url = url, apiKey = apiKey)

            _uiState.update {
                it.copy(
                    isUrlValid = true,
                    isConnectionCheckRunning = false,
                    connectionCheckResult = connectionCheckResult,
                )
            }
        }
    }

    fun saveConfig() {
        if (!isUrlValid()) {
            _uiState.update {
                it.copy(isUrlValid = false, isConfigSaved = false)
            }
            return
        }

        viewModelScope.launch {
            if (searchProviderId == null) {
                createConfig()
            } else {
                updateConfig(searchProviderId)
            }

            _uiState.update { it.copy(isUrlValid = true, isConfigSaved = true) }
        }
    }

    private fun isUrlValid(): Boolean = Patterns.WEB_URL.matcher(_uiState.value.url).matches()

    private suspend fun createConfig() {
        searchProvidersRepository.createTorznabConfig(
            searchProviderName = _uiState.value.searchProviderName,
            url = _uiState.value.url,
            apiKey = _uiState.value.apiKey,
            category = _uiState.value.category,
        )
    }

    private suspend fun updateConfig(id: SearchProviderId) {
        searchProvidersRepository.updateTorznabConfig(
            id = id,
            searchProviderName = _uiState.value.searchProviderName,
            url = _uiState.value.url,
            apiKey = _uiState.value.apiKey,
            category = _uiState.value.category,
        )
    }
}