package com.prajwalch.torrentsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchScreenViewModel : ViewModel() {
    private val repository = TorrentsRepository()

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _contentType = MutableStateFlow(ContentType.All)
    val activeContentType = _contentType.asStateFlow()

    private val _results = MutableStateFlow(emptyList<Torrent>())
    val results = _results.asStateFlow()

    fun setQuery(query: String) {
        _query.value = query
    }

    fun setContentType(contentType: ContentType) {
        _contentType.value = contentType
    }

    fun onSubmit() {
        if (_query.value.isEmpty()) {
            return
        }

        viewModelScope.launch {
            _results.value = repository.search(_query.value, _contentType.value)
        }
    }
}