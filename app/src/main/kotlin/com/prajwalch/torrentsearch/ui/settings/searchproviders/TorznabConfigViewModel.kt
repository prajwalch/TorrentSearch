package com.prajwalch.torrentsearch.ui.settings.searchproviders

import android.util.Patterns

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.repository.SearchProvidersRepository
import com.prajwalch.torrentsearch.domain.models.Category
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
    val isUrlValid: Boolean = true,
    val isConfigSaved: Boolean = false,
) {
    fun isConfigNotBlank() =
        searchProviderName.isNotBlank() && url.isNotBlank() && apiKey.isNotBlank()
}

@HiltViewModel
class TorznabConfigViewModel @Inject constructor(
    private val searchProvidersRepository: SearchProvidersRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val torznabSearchProviderId = savedStateHandle.get<String>("id")

    private val _uiState = MutableStateFlow(TorznabConfigUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            torznabSearchProviderId?.let { loadConfig(it) }
        }
    }

    private suspend fun loadConfig(id: SearchProviderId) {
        val config = searchProvidersRepository.findTorznabConfig(id = id) ?: return

        _uiState.update {
            it.copy(
                searchProviderName = config.searchProviderName,
                url = config.url,
                apiKey = config.apiKey,
                category = config.category,
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

    fun saveConfig() {
        viewModelScope.launch {
            if (!isUrlValid()) {
                _uiState.update {
                    it.copy(isUrlValid = false, isConfigSaved = false)
                }
                return@launch
            }

            val isConfigNew = torznabSearchProviderId == null

            if (isConfigNew) {
                searchProvidersRepository.addTorznabConfig(
                    searchProviderName = _uiState.value.searchProviderName,
                    url = _uiState.value.url,
                    apiKey = _uiState.value.apiKey,
                    category = _uiState.value.category,
                )
            } else {
                searchProvidersRepository.updateTorznabConfig(
                    id = torznabSearchProviderId,
                    searchProviderName = _uiState.value.searchProviderName,
                    url = _uiState.value.url,
                    apiKey = _uiState.value.apiKey,
                    category = _uiState.value.category,
                )
            }

            _uiState.update {
                it.copy(isUrlValid = true, isConfigSaved = true)
            }
        }
    }

    private fun isUrlValid(): Boolean = Patterns.WEB_URL.matcher(_uiState.value.url).matches()
}