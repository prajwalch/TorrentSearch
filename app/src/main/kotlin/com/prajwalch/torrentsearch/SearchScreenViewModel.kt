package com.prajwalch.torrentsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchScreenUIState(
    val query: String = "",
    val contentType: ContentType = ContentType.All,
    val results: List<Torrent> = emptyList(),
    val isLoading: Boolean = false,
)

class SearchScreenViewModel : ViewModel() {
    private val repository = TorrentsRepository()

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _contentType = MutableStateFlow(ContentType.All)
    val activeContentType = _contentType.asStateFlow()

    private val _results = MutableStateFlow(emptyList<Torrent>())
    val results = _results.asStateFlow()

    private val _uiState = MutableStateFlow(SearchScreenUIState())
    val uiState = _uiState.asStateFlow()

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
//        _query.value = query
    }

    fun setContentType(contentType: ContentType) {
        _uiState.update { it.copy(contentType = contentType) }
//        _contentType.value = contentType
    }

    fun onSubmit() {
        val query = _uiState.value.query
        val contentType = _uiState.value.contentType

        if (query.isEmpty()) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val results = repository.search(query, contentType)
            _uiState.update { it.copy(results = results, isLoading = false) }
        }
    }
}