package com.prajwalch.torrentsearch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.prajwalch.torrentsearch.data.Category
import com.prajwalch.torrentsearch.data.Torrent
import com.prajwalch.torrentsearch.data.TorrentsRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchScreenUIState(
    val query: String = "",
    val category: Category = Category.All,
    val results: List<Torrent> = emptyList(),
    val isLoading: Boolean = false,
    val isInternetError: Boolean = false,
)

class SearchScreenViewModel : ViewModel() {
    private val repository = TorrentsRepository()

    private val _uiState = MutableStateFlow(SearchScreenUIState())
    val uiState = _uiState.asStateFlow()

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun setCategory(category: Category) {
        _uiState.update { it.copy(category = category) }
    }

    fun onSubmit() {
        if (_uiState.value.query.isEmpty()) {
            return
        }

        viewModelScope.launch {
            updateUIState { it.copy(isLoading = true) }

            if (!repository.isInternetAvailable()) {
                updateUIState { it.copy(isLoading = false, isInternetError = true) }
                return@launch
            }

            val results = repository.search(_uiState.value.query, _uiState.value.category)
            updateUIState {
                it.copy(results = results, isLoading = false, isInternetError = false)
            }
        }
    }

    private inline fun updateUIState(update: (SearchScreenUIState) -> SearchScreenUIState) {
        _uiState.update(function = update)
    }
}