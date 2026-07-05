package com.prajwalch.torrentsearch.ui.settings.searchproviders.addedit

import android.util.Patterns

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.domain.SearchProvidersManager
import com.prajwalch.torrentsearch.domain.model.Category
import com.prajwalch.torrentsearch.domain.model.TorznabConnectionCheckResult
import com.prajwalch.torrentsearch.providers.SearchProviderId
import com.prajwalch.torrentsearch.torznab.TorznabUtils

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import javax.inject.Inject

data class TorznabConfigUiState(
    val searchProviderName: String = "",
    val url: String = "",
    val apiKey: String = "",
    val supportedCategories: Set<Category> = emptySet(),
    val isNewConfig: Boolean = true,
    val isUrlValid: Boolean = true,
    val isCheckingConnection: Boolean = false,
    val isDetectingSupportedCategories: Boolean = false,
) {
    fun isConfigNotBlank() =
        searchProviderName.isNotBlank() && url.isNotBlank() && apiKey.isNotBlank()
}

sealed interface TorznabConfigEvent {
    data object ConfigSaved : TorznabConfigEvent

    data class ConnectionCheckCompleted(
        val result: TorznabConnectionCheckResult,
    ) : TorznabConfigEvent
}

@HiltViewModel
class TorznabConfigViewModel @Inject constructor(
    private val searchProvidersManager: SearchProvidersManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    /**
     * ID of the search provider whose config to edit (edit mode).
     *
     * if `null`, a new config is created (add mode).
     */
    private val searchProviderId = savedStateHandle.get<String>("id")

    private val _uiState =
        MutableStateFlow(TorznabConfigUiState(isNewConfig = searchProviderId == null))
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<TorznabConfigEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        searchProviderId?.let(::loadConfig)
    }

    private fun loadConfig(id: SearchProviderId) = viewModelScope.launch {
        val config = searchProvidersManager.findTorznabConfigById(id) ?: return@launch

        _uiState.value = TorznabConfigUiState(
            searchProviderName = config.searchProviderName,
            url = config.url,
            apiKey = config.apiKey,
            supportedCategories = config.supportedCategories,
            isNewConfig = false,
        )
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

    fun toggleCategorySelection(category: Category) {
        _uiState.update {
            val supportedCategories = if (category in it.supportedCategories) {
                it.supportedCategories - category
            } else {
                it.supportedCategories + category
            }

            it.copy(supportedCategories = supportedCategories)
        }
    }

    fun detectSupportedCategories() {
        if (!isUrlValid()) {
            _uiState.update { it.copy(isUrlValid = false) }
            return
        } else {
            _uiState.update { it.copy(isUrlValid = true) }
        }

        _uiState.update { it.copy(isDetectingSupportedCategories = true) }
        viewModelScope.launch {
            val apiUrl = _uiState.value.url
            val apiKey = _uiState.value.apiKey

            val connectionCheckResult = TorznabUtils.checkConnection(apiUrl, apiKey)
            if (connectionCheckResult !is TorznabConnectionCheckResult.ConnectionEstablished) {
                _uiState.update { it.copy(isDetectingSupportedCategories = false) }
                _events.send(TorznabConfigEvent.ConnectionCheckCompleted(connectionCheckResult))

                return@launch
            }

            val supportedCategories = TorznabUtils.getSupportedCategories(apiUrl, apiKey)
            _uiState.update {
                it.copy(
                    supportedCategories = supportedCategories.orEmpty(),
                    isDetectingSupportedCategories = false
                )
            }
        }
    }

    fun checkConnection() {
        if (!isUrlValid()) {
            _uiState.update { it.copy(isUrlValid = false) }
            return
        }

        _uiState.update { it.copy(isUrlValid = true, isCheckingConnection = true) }
        viewModelScope.launch {
            val apiUrl = _uiState.value.url
            val apiKey = _uiState.value.apiKey
            val checkResult = TorznabUtils.checkConnection(apiUrl, apiKey)

            _uiState.update { it.copy(isCheckingConnection = false) }
            _events.send(TorznabConfigEvent.ConnectionCheckCompleted(checkResult))
        }
    }

    fun saveConfig() {
        if (!isUrlValid()) {
            _uiState.update { it.copy(isUrlValid = false) }
            return
        } else {
            _uiState.update { it.copy(isUrlValid = true) }
        }

        viewModelScope.launch {
            if (searchProviderId == null) {
                createConfig()
            } else {
                updateConfig(searchProviderId)
            }
            _events.send(TorznabConfigEvent.ConfigSaved)
        }
    }

    private fun isUrlValid(): Boolean = Patterns.WEB_URL.matcher(_uiState.value.url).matches()

    private suspend fun createConfig() {
        searchProvidersManager.createTorznabConfig(
            searchProviderName = _uiState.value.searchProviderName,
            url = _uiState.value.url,
            apiKey = _uiState.value.apiKey,
            supportedCategories = _uiState.value.supportedCategories,
        )
    }

    private suspend fun updateConfig(id: SearchProviderId) {
        searchProvidersManager.updateTorznabConfig(
            id = id,
            searchProviderName = _uiState.value.searchProviderName,
            url = _uiState.value.url,
            apiKey = _uiState.value.apiKey,
            supportedCategories = _uiState.value.supportedCategories,
        )
    }
}