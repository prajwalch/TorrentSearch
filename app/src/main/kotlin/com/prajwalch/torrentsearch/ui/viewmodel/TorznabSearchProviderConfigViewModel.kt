package com.prajwalch.torrentsearch.ui.viewmodel

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

@HiltViewModel
class TorznabSearchProviderConfigViewModel @Inject constructor(
    private val searchProvidersRepository: SearchProvidersRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val torznabSearchProviderId = savedStateHandle.get<String>("id")

    private val _uiState = MutableStateFlow(
        TorznabSearchProviderConfig(
            id = "",
            name = "",
            url = "",
            apiKey = "",
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        loadExistingConfig()
    }

    private fun loadExistingConfig() = viewModelScope.launch {
        val id = torznabSearchProviderId ?: return@launch
        val existingConfig = searchProvidersRepository
            .findTorznabSearchProviderConfig(id = id)
            ?: return@launch

        _uiState.value = existingConfig
    }

    fun changeName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun changeUrl(url: String) {
        _uiState.update { it.copy(url = url) }
    }

    fun changeAPIKey(apiKey: String) {
        _uiState.update { it.copy(apiKey = apiKey) }
    }

    fun changeCategory(category: Category) {
        _uiState.update { it.copy(category = category) }
    }

    fun changeSafetyStatus(safetyStatus: SearchProviderSafetyStatus) {
        _uiState.update { it.copy(safetyStatus = safetyStatus) }
    }

    fun save() {
        viewModelScope.launch {
            val isNewConfig = torznabSearchProviderId == null

            if (isNewConfig) {
                searchProvidersRepository.addTorznabSearchProvider(config = _uiState.value)
            } else {
                searchProvidersRepository.updateTorznabSearchProvider(config = _uiState.value)
            }
        }
    }
}